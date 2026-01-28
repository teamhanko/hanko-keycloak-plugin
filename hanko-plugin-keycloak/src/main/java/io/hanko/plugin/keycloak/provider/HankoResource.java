package io.hanko.plugin.keycloak.provider;

import io.hanko.plugin.keycloak.HankoClientFactory;
import io.hanko.plugin.keycloak.HankoLogger;
import io.hanko.plugin.keycloak.SupportedProtocols;
import io.hanko.plugin.keycloak.user.User;
import io.hanko.plugin.keycloak.user.UserFactory;
import io.hanko.sdk.HankoClient;
import io.hanko.sdk.models.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

import java.util.UUID;

public class HankoResource {

    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;

    public HankoResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
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
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

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
            req.setClientData(getClientData());

            HankoRequest hankoRequest = hankoClient.requestWebAuthnOperation(req);
            user.setEnrollmentWebAuthnRequestId(hankoRequest.getId());

            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.registrationFailed(SupportedProtocols.WEBAUTHN, user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not create Hanko WebAuthn request: " + ex.getMessage());
            return response.build();
        }
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
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

        String requestId = user.getEnrollmentWebAuthnRequestId();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            HankoRequest hankoRequest = hankoClient.validateWebAuthnRequest(requestId, webAuthnResponse);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.verificationFailed(SupportedProtocols.WEBAUTHN, requestId, user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not verify Hanko WebAuthn request: " + ex.getMessage());
            return response.build();
        }
    }

    @POST
    @Path("/{realmName}/{userId}/uaf/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUaf(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId
    ) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

        try {
            if (user.getHankoUserId() == null) {
                user.setHankoUserId(UUID.randomUUID().toString());
            }

            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);

            CreateUafRequest req = new CreateUafRequest();
            req.setOperation(Operation.REG);
            req.setUsername(user.delegate().getUsername());
            req.setUserId(user.getHankoUserId());
            req.setClientData(getClientData());

            HankoRequest hankoRequest = hankoClient.requestUafOperation(req);
            user.setEnrollmentUafRequestId(hankoRequest.getId());

            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.registrationFailed(SupportedProtocols.UAF, user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not create Hanko UAF request: " + ex.getMessage());
            return response.build();
        }
    }

    private void requireManage(RealmModel realm, User user) {
        AdminPermissions.evaluator(session, realm, auth.adminAuth()).users().requireManage(user.delegate());
    }

    @GET
    @Path("/{realmName}/{userId}/uaf/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUafRequest(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId
    ) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

        String requestId = user.getEnrollmentUafRequestId();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            HankoRequest hankoRequest = hankoClient.getUafRequest(requestId);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.uafStatusRequestFailed(requestId, user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity(String.format("Could not get Hanko UAF request with ID %s: %s", requestId, ex.getMessage()));
            return response.build();
        }
    }

    @DELETE
    @Path("/{realmName}/{userId}/uaf/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cancelUaf(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId
    ) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

        String requestId = user.getEnrollmentUafRequestId();

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            HankoRequest hankoRequest = hankoClient.cancelUafRequest(requestId);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.uafCancelRequestFailed(user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not cancel Hanko UAF request: " + ex.getMessage());
            return response.build();
        }
    }

    @GET
    @Path("/{realmName}/{userId}/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices(
            @PathParam("realmName") String realmName,
            @PathParam("userId") String userId)
    {
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            Device[] devices = hankoClient.getRegisteredDevices(user.getHankoUserId());
            Response.ResponseBuilder response = Response.ok(devices);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.getRegisteredDevicesFailed(user, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not retrieve user's devices: " + ex.getMessage());
            return response.build();
        }
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
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);

            CreateUafRequest req = new CreateUafRequest();
            req.setOperation(Operation.DEREG);
            req.setUsername(user.delegate().getUsername());
            req.setUserId(user.getHankoUserId());
            req.setClientData(getClientData());
            req.setDeviceIds(new String[]{deviceId});

            HankoRequest hankoRequest = hankoClient.requestUafOperation(req);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.deregistrationFailed(SupportedProtocols.UAF, user, deviceId, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not deregister device: " + ex.getMessage());
            return response.build();
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
        return renameDevice(realmName, userId, deviceId, renameDevice);
    }

    private Response renameDevice(String realmName, String userId, String deviceId, RenameDevice renameDevice) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);
            RenameDevice renameResponse = hankoClient.renameDevice(deviceId, renameDevice);
            Response.ResponseBuilder response = Response.ok(renameResponse);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.renameDeviceFailed(user, deviceId, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not rename device: " + ex.getMessage());
            return response.build();
        }
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
        RealmModel realm = session.realms().getRealmByName(realmName);
        User user = UserFactory.getUser(session, userId, realm);
        requireManage(realm, user);

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(realm);

            CreateWebAuthnRequest req = new CreateWebAuthnRequest();
            req.setOperation(Operation.DEREG);
            req.setUsername(user.delegate().getUsername());
            req.setUserId(user.getHankoUserId());
            req.setClientData(getClientData());
            req.setDeviceIds(new String[]{deviceId});

            HankoRequest hankoRequest = hankoClient.requestWebAuthnOperation(req);
            Response.ResponseBuilder response = Response.ok(hankoRequest);
            return response.build();
        } catch (Exception ex) {
            HankoLogger.deregistrationFailed(SupportedProtocols.WEBAUTHN, user, deviceId, ex);
            Response.ResponseBuilder response = Response.serverError()
                    .entity("Could not deregister device: " + ex.getMessage());
            return response.build();
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
        return renameDevice(realmName, userId, deviceId, renameDevice);
    }

    private ClientData getClientData() {
        String remoteAddress = session.getContext().getConnection().getRemoteAddr();
        ClientData clientData = new ClientData();
        clientData.setRemoteAddress(remoteAddress);
        return clientData;
    }
}
