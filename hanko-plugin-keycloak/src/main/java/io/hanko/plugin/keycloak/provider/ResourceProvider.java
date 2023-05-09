package io.hanko.plugin.keycloak.provider;

import io.hanko.plugin.keycloak.HankoClientFactory;
import io.hanko.plugin.keycloak.config.ConfigException;
import io.hanko.plugin.keycloak.user.User;
import io.hanko.plugin.keycloak.user.UserFactory;
import io.hanko.sdk.HankoClient;
import io.hanko.sdk.models.ClientData;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;

import java.util.UUID;

public class ResourceProvider implements RealmResourceProvider {
    public final KeycloakSession session;
    public final KeycloakContext context;
    public HttpRequest request;

    ResourceProvider(KeycloakSession session) {
        this.session = session;
        this.context = session.getContext();
        this.request = session.getContext().getHttpRequest();
    }

    HankoClient getSessionHankoClient() throws ConfigException {
        return HankoClientFactory.getHankoClient(session);
    }

    ClientData getContextClientData() {
        return ClientDataFactory.getClientData(context);
    }

    User getSessionUser() {
        return UserFactory.getUser(session);
    }

    User getRealmUser(String userId, RealmModel realm) {
        User user = UserFactory.getUser(session, userId, realm);
        if (user.getHankoUserId() == null) {
            user.setHankoUserId(UUID.randomUUID().toString());
        }
        user.setEnrollmentUafRequestId(null);
        return user;
    }

    AccessToken getAccessToken(KeycloakSession session) {
        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        return authResult.getToken();
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }

}
