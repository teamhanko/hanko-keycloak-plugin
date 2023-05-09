package io.hanko.plugin.keycloak.provider;

import io.hanko.plugin.keycloak.HankoClientFactory;
import io.hanko.plugin.keycloak.HankoLogger;
import io.hanko.plugin.keycloak.SupportedProtocols;
import io.hanko.plugin.keycloak.config.ConfigException;
import io.hanko.plugin.keycloak.user.User;
import io.hanko.plugin.keycloak.user.UserFactory;
import io.hanko.sdk.HankoClient;
import io.hanko.sdk.models.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class HankoProvider extends ResourceProvider {

    HankoProvider(KeycloakSession session) {
        super(session);
    }

    @OPTIONS
    @Path("/{realmName}/{userId}/webauthn/register")
    public Response registerWebAuthnPreflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("POST").auth().build();
    }

    @POST
    @Path("/{realmName}/{userId}/webauthn/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerWebAuthn(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId,
            AuthenticatorSelectionCriteria authenticatorSelectionCriteria
    ) {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("POST")
                .auth();

        try {
            if (user.getHankoUserId() == null) {
                user.setHankoUserId(UUID.randomUUID().toString());
            }

            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);

            CreateWebAuthnRequest req = new CreateWebAuthnRequest();
            req.setOperation(Operation.REG);
            req.setUsername(user.delegate().getUsername());
            req.setUserId(user.getHankoUserId());
            req.setAuthenticatorSelectionCriteria(authenticatorSelectionCriteria);
            req.setClientData(getContextClientData());

            HankoRequest hankoRequest = hankoClient.requestWebAuthnOperation(req);
            user.setEnrollmentWebAuthnRequestId(hankoRequest.getId());

            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.registrationFailed(SupportedProtocols.WEBAUTHN, user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not create Hanko WebAuthn request.");
            return cors.builder(response).build();
        }
    }

    @OPTIONS
    @Path("/{realmName}/{userId}/webauthn/verify")
    public Response verifyWebAuthnPreflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("POST").auth().build();
    }

    @POST
    @Path("/{realmName}/{userId}/webauthn/verify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verifyWebAuthn(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId,
            String webAuthnResponse)
    {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        String requestId = user.getEnrollmentWebAuthnRequestId();

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("POST")
                .auth();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            HankoRequest hankoRequest = hankoClient.validateWebAuthnRequest(requestId, webAuthnResponse);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.verificationFailed(SupportedProtocols.WEBAUTHN, requestId, user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not verify Hanko WebAuthn request.");
            return cors.builder(response).build();
        }
    }

    @OPTIONS
    @Path("/{realmName}/{userId}/uaf/register")
    public Response registerUafPreflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("POST").auth().build();
    }

    @POST
    @Path("/{realmName}/{userId}/uaf/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUaf(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId
    ) {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("POST")
                .auth();

        try {
            if (user.getHankoUserId() == null) {
                user.setHankoUserId(UUID.randomUUID().toString());
            }

            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);

            CreateUafRequest req = new CreateUafRequest();
            req.setOperation(Operation.REG);
            req.setUsername(user.delegate().getUsername());
            req.setUserId(user.getHankoUserId());
            req.setClientData(getContextClientData());

            HankoRequest hankoRequest = hankoClient.requestUafOperation(req);
            user.setEnrollmentUafRequestId(hankoRequest.getId());

            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.registrationFailed(SupportedProtocols.UAF, user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not create Hanko UAF request.");
            return cors.builder(response).build();
        }
    }

    @OPTIONS
    @Path("/{realmName}/{userId}/uaf/status")
    public Response getUafRequestPreflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("GET").auth().build();
    }

    @GET
    @Path("/{realmName}/{userId}/uaf/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUafRequest(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId
    ) {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        String requestId = user.getEnrollmentUafRequestId();

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("GET")
                .auth();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            HankoRequest hankoRequest = hankoClient.getUafRequest(requestId);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.registrationFailed(SupportedProtocols.UAF, user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not get Hanko UAF request.");
            return cors.builder(response).build();
        }
    }

    @OPTIONS
    @Path("/{realmName}/{userId}/uaf/cancel")
    public Response cancelUafPreflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("DELETE").auth().build();
    }

    @DELETE
    @Path("/{realmName}/{userId}/uaf/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cancelUaf(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId
    ) {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        String requestId = user.getEnrollmentUafRequestId();

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("DELETE")
                .auth();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            HankoRequest hankoRequest = hankoClient.cancelUafRequest(requestId);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return cors.builder(response).build();
        } catch (ConfigException ex) {
            HankoLogger.uafCancelRequestFailed(user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not cancel Hanko UAF request.");
            return cors.builder(response).build();
        }
    }

    @OPTIONS
    @Path("/{realmName}/{userId}/devices")
    public Response getDevicesPreflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("GET").auth().build();
    }

    @GET
    @Path("/{realmName}/{userId}/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId)
    {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("GET")
                .auth();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            Device[] devices = hankoClient.getRegisteredDevices(user.getHankoUserId());
            Response.ResponseBuilder response = Response.ok(devices);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.getRegisteredDevicesFailed(user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not retrieve user's devices");
            return cors.builder(response).build();
        }
    }

    @OPTIONS
    @Path("/{realmName}/{userId}/devices/uaf/{deviceId}")
    public Response uafDevicePreflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("DELETE", "PUT").auth().build();
    }

    @DELETE
    @Path("/{realmName}/{userId}/devices/uaf/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deregisterUafDevice(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId
    ) {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("DELETE")
                .auth();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);

            CreateUafRequest req = new CreateUafRequest();
            req.setOperation(Operation.DEREG);
            req.setUsername(user.delegate().getUsername());
            req.setUserId(user.getHankoUserId());
            req.setClientData(getContextClientData());
            req.setDeviceIds(new String[]{deviceId});

            HankoRequest hankoRequest = hankoClient.requestUafOperation(req);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.deregistrationFailed(SupportedProtocols.UAF, user, deviceId, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not deregister device.");
            return cors.builder(response).build();
        }
    }

    @PUT
    @Path("/{realmName}/{userId}/devices/uaf/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response renameUafDevice(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId,
            RenameDevice renameDevice
    ) {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("PUT")
                .auth();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            RenameDevice renameResponse = hankoClient.renameDevice(deviceId, renameDevice);
            Response.ResponseBuilder response = Response.ok(renameResponse);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.renameDeviceFailed(user, deviceId, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not rename device.");
            return cors.builder(response).build();
        }
    }

    @OPTIONS
    @Path("/{realmName}/{userId}/devices/webauthn/{deviceId}")
    public Response webauthnDevicePreflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("DELETE", "PUT").auth().build();
    }

    @DELETE
    @Path("/{realmName}/{userId}/devices/webauthn/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deregisterWebauthnDevice(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId
    ) {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("DELETE")
                .auth();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);

            CreateWebAuthnRequest req = new CreateWebAuthnRequest();
            req.setOperation(Operation.DEREG);
            req.setUsername(user.delegate().getUsername());
            req.setUserId(user.getHankoUserId());
            req.setClientData(getContextClientData());
            req.setDeviceIds(new String[]{deviceId});

            HankoRequest hankoRequest = hankoClient.requestWebAuthnOperation(req);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.deregistrationFailed(SupportedProtocols.WEBAUTHN, user, deviceId, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not deregister device.");
            return cors.builder(response).build();
        }
    }

    @PUT
    @Path("/{realmName}/{userId}/devices/webauthn/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response renameWebAuthnDevice(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId,
            @PathParam("deviceId") String deviceId,
            RenameDevice renameDevice
    ) {
        User sessionUser = getSessionUser();
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);

        Cors cors = new Cors(request)
                .allowedOrigins(getAccessToken(session))
                .allowedMethods("PUT")
                .auth();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            RenameDevice renameResponse = hankoClient.renameDevice(deviceId, renameDevice);
            Response.ResponseBuilder response = Response.ok(renameResponse);
            return cors.builder(response).build();
        } catch (Exception ex) {
            HankoLogger.renameDeviceFailed(user, deviceId, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not rename device.");
            return cors.builder(response).build();
        }
    }
}
