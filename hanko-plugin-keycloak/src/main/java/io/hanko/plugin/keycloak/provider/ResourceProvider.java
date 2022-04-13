package io.hanko.plugin.keycloak.provider;

import io.hanko.plugin.keycloak.HankoClientFactory;
import io.hanko.plugin.keycloak.config.ConfigException;
import io.hanko.plugin.keycloak.user.User;
import io.hanko.plugin.keycloak.user.UserFactory;
import io.hanko.sdk.HankoClient;
import io.hanko.sdk.models.ClientData;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;

import javax.ws.rs.core.Context;
import java.util.UUID;

public class ResourceProvider implements RealmResourceProvider {
    public final KeycloakSession session;
    public final KeycloakContext context;

    ResourceProvider(KeycloakSession session) {
        this.session = session;
        this.context = session.getContext();
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

    @Context
    public HttpRequest request;

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }

}
