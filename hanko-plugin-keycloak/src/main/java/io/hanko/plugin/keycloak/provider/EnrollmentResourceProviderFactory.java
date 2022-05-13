package io.hanko.plugin.keycloak.provider;

import io.hanko.plugin.keycloak.HankoLogger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class EnrollmentResourceProviderFactory implements RealmResourceProviderFactory {

    private static final String ID = "enrollment";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        try {
            return new EnrollmentResourceProvider(session);
        } catch (Exception e) {
            HankoLogger.clientConfigFailed();
            return new EnrollmentResourceProvider(session);
        }
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}