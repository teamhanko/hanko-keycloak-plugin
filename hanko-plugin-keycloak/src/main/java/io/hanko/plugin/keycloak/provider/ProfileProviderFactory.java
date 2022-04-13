package io.hanko.plugin.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.theme.FreeMarkerUtil;

public class ProfileProviderFactory implements RealmResourceProviderFactory //, ConfiguredProvider
{
    private static final String ID = "hanko";

    private static ServicesLogger logger = ServicesLogger.LOGGER;

    private FreeMarkerUtil freeMarker;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        try{
            ProfileProvider provider = new ProfileProvider(session, freeMarker);
            return provider;
        } catch (Exception e) {
            logger.warn("Configuration Exception for HankoProfileProvider.");
            return new ProfileProvider(session, freeMarker);
        }
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        freeMarker = null;
    }
}
