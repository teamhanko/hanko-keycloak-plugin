package io.hanko.plugin.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class HankoAuthResourceProviderFactory implements RealmResourceProviderFactory, RealmResourceProvider {
    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new HankoAuthResourceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "hanko-auth";
    }

    @Override
    public Object getResource() {
        return this;
    }
}
