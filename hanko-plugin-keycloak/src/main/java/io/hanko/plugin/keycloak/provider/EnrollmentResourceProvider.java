package io.hanko.plugin.keycloak.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.hanko.plugin.keycloak.HankoClientFactory;
import io.hanko.plugin.keycloak.HankoLogger;
import io.hanko.plugin.keycloak.SupportedProtocols;
import io.hanko.plugin.keycloak.config.ConfigException;
import io.hanko.plugin.keycloak.user.User;
import io.hanko.sdk.HankoClient;
import io.hanko.sdk.models.*;
import org.keycloak.models.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public class EnrollmentResourceProvider extends ResourceProvider {

    EnrollmentResourceProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public Object getResource() {
        return this;
    }

    @POST
    @Path("/{realmId}/uaf")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUaf(
            @PathParam("realmId") String userToEnrollRealmId,
            @QueryParam("userId") String userIdKeycloak,
            AuthenticatorSelectionCriteria authenticatorSelectionCriteria
    ) {
        User user = getSessionUser();
        RealmModel userToEnrollRealm = session.realms().getRealm(userToEnrollRealmId);
        User userToEnroll = getRealmUser(userIdKeycloak, userToEnrollRealm);

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(userToEnrollRealm);

            CreateUafRequest req = new CreateUafRequest();
            req.setOperation(Operation.REG);
            req.setUsername(userToEnroll.delegate().getUsername());
            req.setUserId(userToEnroll.getHankoUserId());
            req.setClientData(getContextClientData());

            HankoRequest hankoRequest = hankoClient.requestUafOperation(req);
            userToEnroll.setEnrollmentUafRequestId(hankoRequest.getId());

            return Response.ok(hankoRequest).build();
        } catch (ConfigException ex) {
            HankoLogger.registrationFailed(SupportedProtocols.UAF, user, ex);
            return Response.serverError().entity("Could not request Hanko registration.").build();
        }
    }

    @POST
    @Path("/{realmId}/webauthn")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerWebAuthn(
            @PathParam("realmId") String userToEnrollRealmId,
            @QueryParam("userId") String userIdKeycloak,
            AuthenticatorSelectionCriteria authenticatorSelectionCriteria
    ) {
        User user = getSessionUser();
        RealmModel userToEnrollRealm = session.realms().getRealm(userToEnrollRealmId);
        User userToEnroll = getRealmUser(userIdKeycloak, userToEnrollRealm);

        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(userToEnrollRealm);
            CreateWebAuthnRequest req = new CreateWebAuthnRequest();
            req.setOperation(Operation.REG);
            req.setUsername(userToEnroll.delegate().getUsername());
            req.setUserId(userToEnroll.getHankoUserId());
            req.setAuthenticatorSelectionCriteria(authenticatorSelectionCriteria);
            req.setClientData(getContextClientData());

            HankoRequest hankoRequest = hankoClient.requestWebAuthnOperation(req);
            userToEnroll.setEnrollmentWebAuthnRequestId(hankoRequest.getId());

            return Response.ok(hankoRequest).build();
        } catch (ConfigException ex) {
            HankoLogger.registrationFailed(SupportedProtocols.WEBAUTHN, user, ex);
            return Response.serverError().entity("Could not verify Hanko request.").build();
        }
    }

    @PUT
    @Path("/{realmId}/webauthn/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerVerify(
            @PathParam("realmId") String userToEnrollRealmId,
            @PathParam("requestId") String requestId,
            String webAuthnResponse) {
        User user = getSessionUser();
        try {
            RealmModel userToEnrollRealm = session.realms().getRealm(userToEnrollRealmId);
            HankoClient hankoClient = HankoClientFactory.getHankoClient(userToEnrollRealm);
            HankoRequest hankoRequest = hankoClient.validateWebAuthnRequest(requestId, webAuthnResponse);
            return Response.ok(hankoRequest).build();
        } catch (ConfigException | JsonProcessingException ex) {
            HankoLogger.verificationFailed(SupportedProtocols.WEBAUTHN, requestId, user, ex);
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/{realmId}/uaf/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cancelUaf(
            @PathParam("realmId") String userToEnrollRealmId,
            @PathParam("requestId") String requestId,
            @QueryParam("userId") String userIdKeycloak) {
        User user = getSessionUser();
        try {
            RealmModel userToEnrollRealm = session.realms().getRealm(userToEnrollRealmId);
            HankoClient hankoClient = HankoClientFactory.getHankoClient(userToEnrollRealm);
            HankoRequest hankoRequest = hankoClient.cancelUafRequest(requestId);
            return Response.ok(hankoRequest).build();
        } catch (ConfigException ex) {
            HankoLogger.uafCancelRequestFailed(user, ex);
            return Response.serverError().entity("Could not cancel Hanko request.").build();
        }
    }

    @GET
    @Path("/{realmId}/uaf/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getRequestStatus(
            @PathParam("realmId") String userToEnrollRealmId,
            @PathParam("requestId") String requestId) {
        User user = getSessionUser();
        try {
            RealmModel userToEnrollRealm = session.realms().getRealm(userToEnrollRealmId);
            HankoClient hankoClient = HankoClientFactory.getHankoClient(userToEnrollRealm);
            HankoRequest request = hankoClient.getUafRequest(requestId);
            return Response.ok(request).build();
        } catch (ConfigException ex) {
            HankoLogger.verificationFailed(SupportedProtocols.UAF, requestId, user, ex);
            return Response.serverError().entity("Could not get Hanko request.").build();
        }
    }

    @Override
    public void close() {
        //noop
    }
}