package io.hanko.plugin.keycloak.provider;

import io.hanko.plugin.keycloak.HankoLogger;
import io.hanko.plugin.keycloak.SupportedProtocols;
import io.hanko.plugin.keycloak.authenticator.HankoCredentialModel;
import io.hanko.plugin.keycloak.serialization.ChangePasswordRequest;
import io.hanko.plugin.keycloak.serialization.HankoStatusResponse;
import io.hanko.plugin.keycloak.user.User;
import io.hanko.sdk.models.*;
import org.keycloak.forms.account.freemarker.model.RealmBean;
import org.keycloak.forms.account.freemarker.model.UrlBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.services.resources.Cors;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileProvider extends ResourceProvider {

    private FreeMarkerUtil freeMarker;

    ProfileProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        super(session);
        this.freeMarker = freeMarker;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @OPTIONS
    @Path("{any:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersionPreflight() {
        if (request == null) {
            // logger.error("Request null"); TODO: needed?
        }
        return Cors.add(request, Response.ok())
                .allowedMethods("GET", "POST", "DELETE")
                .allowedOrigins(session, context.getClient())
                .preflight()
                .auth()
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        User user = getSessionUser();

        boolean isConfiguredForHanko = session.userCredentialManager().isConfiguredFor(
                context.getRealm(), user.delegate(), HankoCredentialModel.TYPE);

        try {
            boolean hasRegisteredDevices = false;

            String hankoUserId = user.getHankoUserId();
            if (hankoUserId != null) {
                hasRegisteredDevices = getSessionHankoClient().getRegisteredDevices(hankoUserId).length > 0;
            }

            HankoStatusResponse status = new HankoStatusResponse(isConfiguredForHanko && hasRegisteredDevices);
            return Response.ok(status).build();

        } catch (Exception ex) {
            HankoLogger.getRegisteredDevicesFailed(user, ex);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("register")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDevice() {
        return createUafDevice(); // TODO: do we need this?
    }

    @POST
    @Path("registerType/FIDO_UAF")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUafDevice() {
        User user = getSessionUser();
        user.setHankoRequestId(null);

        try {
            CreateUafRequest req = new CreateUafRequest();

            req.setUserId(user.getHankoUserId());
            req.setUsername(user.delegate().getUsername());
            req.setClientData(getContextClientData());
            req.setOperation(Operation.REG);

            HankoRequest hankoRequest = getSessionHankoClient().requestUafOperation(req);
            user.setHankoRequestId(hankoRequest.getId());

            return Response.ok(hankoRequest).build();
        } catch (Exception e) {
            HankoLogger.registrationFailed(SupportedProtocols.UAF, user, e);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("registerType/WEBAUTHN")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWebAuthnDevice(AuthenticatorSelectionCriteria authenticatorSelectionCriteria) {
        User user = getSessionUser();
        user.setHankoRequestId(null);

        try {
            CreateWebAuthnRequest req = new CreateWebAuthnRequest();

            req.setUserId(user.getHankoUserId());
            req.setUsername(user.delegate().getUsername());
            req.setClientData(getContextClientData());
            req.setOperation(Operation.REG);
            req.setAuthenticatorSelectionCriteria(authenticatorSelectionCriteria);
            HankoRequest hankoRequest = getSessionHankoClient().requestWebAuthnOperation(req);
            user.setHankoRequestId(hankoRequest.getId());

            return Response.ok(hankoRequest).build();

        } catch (Exception ex) {
            HankoLogger.registrationFailed(SupportedProtocols.WEBAUTHN, user, ex);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("request/verify/webauthn")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerVerify(String webAuthnResponse) {
        User user = getSessionUser();
        String requestId = user.getHankoRequestId();

        try {
            HankoRequest hankoRequest = getSessionHankoClient().validateWebAuthnRequest(requestId, webAuthnResponse);
            Response.ResponseBuilder responseBuilder = Response.ok(hankoRequest);

            if (hankoRequest.getStatus() == Status.OK) {
                URI uri = context.getUri().getBaseUriBuilder().path("realms").path(context.getRealm().getName()).build();
                int maxCookieAge = 60 * 60 * 24 * 365; // 365 days
                responseBuilder.cookie(new NewCookie("LOGIN_METHOD", SupportedProtocols.WEBAUTHN.name(), uri.getRawPath(), null, null, maxCookieAge, false, true));
            }

            return responseBuilder.build();
        } catch (Exception ex) {
            HankoLogger.verificationFailed(SupportedProtocols.WEBAUTHN, requestId, user, ex);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("register/complete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeUafRegistration() {
        User user = getSessionUser();

        try {
            HankoRequest hankoRequest = getSessionHankoClient().getUafRequest(user.getHankoRequestId());
            Response.ResponseBuilder responseBuilder = Response.ok(hankoRequest);

            if (hankoRequest.getStatus() == Status.OK) {

                List<org.keycloak.credential.CredentialModel> existingCredentials = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), user.delegate(), HankoCredentialModel.TYPE);
                if (existingCredentials.isEmpty()) {
                    HankoCredentialModel credentials = new HankoCredentialModel(user.getHankoUserId());
                    credentials.setType(HankoCredentialModel.TYPE);
                    session.userCredentialManager().createCredential(context.getRealm(), user.delegate(), credentials);
                }

                URI uri = context.getUri().getBaseUriBuilder().path("realms").path(context.getRealm().getName()).build();
                int maxCookieAge = 60 * 60 * 24 * 365; // 365 days
                responseBuilder.cookie(new NewCookie("LOGIN_METHOD", SupportedProtocols.UAF.name(), uri.getRawPath(), null, null, maxCookieAge, false, true));
            }
            return responseBuilder.build();
        } catch (Exception ex) {
            HankoLogger.registrationFailed(SupportedProtocols.UAF, user, ex);
            return Response.serverError().entity("Could not request Hanko registration").build();
        }
    }

    @POST
    @Path("deregister")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deregisterUafDevice() {
        User user = getSessionUser();
        user.setHankoRequestId(null);

        try {
            CreateUafRequest req = new CreateUafRequest();
            req.setUserId(user.getHankoUserId());
            req.setUsername(user.delegate().getUsername());
            req.setClientData(getContextClientData());
            req.setOperation(Operation.REG);
            getSessionHankoClient().requestUafOperation(req);
        } catch (Exception ex) {
            HankoLogger.deregistrationFailed(SupportedProtocols.UAF, user, "*", ex);
            return Response.serverError().entity("Could not deregister device.").build();
        }

        session.userCredentialManager().disableCredentialType(context.getRealm(), user.delegate(), HankoCredentialModel.TYPE);
        HankoStatusResponse hankoStatusResponse = new HankoStatusResponse(false);
        return Response.ok(hankoStatusResponse).build();
    }

    @GET
    @Path("status")
    @Produces(MediaType.TEXT_HTML)
    public String account() {
        String templateName = "account-hanko.ftl";
        Map<String, Object> attributes = new HashMap<>();

        try {
            Theme theme = session.theme().getTheme(Theme.Type.ACCOUNT);
            UriInfo uriInfo = session.getContext().getUri();

            String redirectUrl = uriInfo.getQueryParameters().getFirst("redirect_url");
            if (redirectUrl != null && !redirectUrl.equals("")) {
                attributes.put("redirect_url", redirectUrl);
            }

            String redirectName = uriInfo.getQueryParameters().getFirst("redirect_name");
            if (redirectName != null && !redirectName.equals("")) {
                attributes.put("redirect_name", redirectName);
            }

            URI baseUri = uriInfo.getBaseUri();
            UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
            for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
                baseUriBuilder.queryParam(e.getKey(), e.getValue().toArray());
            }
            URI baseQueryUri = baseUriBuilder.build();
            String stateChecker = (String) session.getAttribute("state_checker");
            if (stateChecker != null) {
                attributes.put("stateChecker", stateChecker);
            }

            UrlBean url = new UrlBean(session.getContext().getRealm(), theme, baseUri, baseQueryUri,
                    uriInfo.getRequestUri(), stateChecker);

            try {
                attributes.put("properties", theme.getProperties());
            } catch (IOException e) {
                HankoLogger.setAttributeFailed("properties", e);
            }

            attributes.put("realm", new RealmBean(context.getRealm()));
            attributes.put("keycloakUrl", baseUri);
            attributes.put("keycloakRealm", context.getRealm().getName());
            attributes.put("keycloakRealmId", context.getRealm().getId());
            attributes.put("keycloakClientId", "hanko-account");
            attributes.put("url", url);

            return freeMarker.processTemplate(attributes, templateName, theme);
        } catch (FreeMarkerException e) {
            HankoLogger.processTemplateFailed(templateName, e);
        } catch (IOException e) {
            HankoLogger.loadThemeFailed(e);
        }

        return "";
    }

    @GET
    @Path("request/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response waitForRequest(@PathParam("requestId") String requestId) {
        try {
            HankoRequest hankoRequest = getSessionHankoClient().getUafRequest(requestId);
            return Response.ok(hankoRequest).build();
        } catch (Exception ex) {
            return Response.serverError().entity("Error while waiting for Hanko request to finish.").build();
        }
    }

    @GET
    @Path("devices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices() {
        User user = getSessionUser();
        try {
            Device[] devices = getSessionHankoClient().getRegisteredDevices(user.getHankoUserId());
            return Response.ok(devices).build();
        } catch (Exception ex) {
            HankoLogger.getRegisteredDevicesFailed(user, ex);
            return Response.serverError().entity("Could not retrieve users devices").build();
        }
    }

    @POST
    @Path("devices/{deviceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renameDevice(@PathParam("deviceId") String deviceId, RenameDevice renameDevice) {
        User user = getSessionUser();
        try {
            getSessionHankoClient().renameDevice(deviceId, renameDevice);
            return Response.ok(renameDevice).build();
        } catch (Exception ex) {
            HankoLogger.renameDeviceFailed(user, deviceId, ex);
            return Response.serverError().entity("Could not rename device.").build();
        }
    }

    @DELETE
    @Path("devices/FIDO_UAF/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUafDevice(@PathParam("deviceId") String deviceId) {
        User user = getSessionUser();
        try {
            CreateUafRequest req = new CreateUafRequest();
            req.setUserId(user.getHankoUserId());
            req.setUsername(user.delegate().getUsername());
            req.setClientData(getContextClientData());
            req.setOperation(Operation.DEREG);
            req.setDeviceIds(new String[]{deviceId});
            HankoRequest request = getSessionHankoClient().requestUafOperation(req);
            return Response.ok(request).build();
        } catch (Exception ex) {
            HankoLogger.deregistrationFailed(SupportedProtocols.UAF, user, deviceId, ex);
            return Response.serverError().entity("Could not delete users device.").build();
        }
    }

    @DELETE
    @Path("devices/WEBAUTHN/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteWebAuthnDevice(@PathParam("deviceId") String deviceId) {
        User user = getSessionUser();
        try {
            CreateWebAuthnRequest req = new CreateWebAuthnRequest();
            req.setUserId(user.getHankoUserId());
            req.setUsername(user.delegate().getUsername());
            req.setClientData(getContextClientData());
            req.setOperation(Operation.DEREG);
            req.setDeviceIds(new String[]{deviceId});
            HankoRequest request = getSessionHankoClient().requestWebAuthnOperation(req);
            return Response.ok(request).build();
        } catch (Exception ex) {
            HankoLogger.deregistrationFailed(SupportedProtocols.WEBAUTHN, user, deviceId, ex);
            return Response.serverError().entity("Could not delete users device").build();
        }
    }

    @POST
    @Path("password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordRequest changePasswordRequest) {
        User user = getSessionUser();
        UserCredentialModel credentials = UserCredentialModel.password(changePasswordRequest.newPassword);
        session.userCredentialManager().updateCredential(context.getRealm(), user.delegate(), credentials);
        return Response.ok().type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() {
        try {
            return Response.ok().build();
        } catch (Exception ex) {
            return Response.serverError().entity("Could not get config").build();
        }
    }

}
