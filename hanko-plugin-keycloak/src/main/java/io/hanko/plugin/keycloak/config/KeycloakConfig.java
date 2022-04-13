package io.hanko.plugin.keycloak.config;

import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;

public class KeycloakConfig extends AbstractConfig<KeycloakSession> {

    KeycloakConfig(KeycloakSession source) {
        super(source);
    }

    public String getApiUrl() throws ConfigException {
        return getNonEmptyConfigValue(this.source, CONFIG_API_URL, "API url");
    }

    public String getApiKey() throws ConfigException {
        return getNonEmptyConfigValue(this.source, CONFIG_APIKEY, "API key secret");
    }

    public String getApiKeyId() throws ConfigException {
        return getNonEmptyConfigValue(this.source, CONFIG_APIKEYID, "API key id");
    }

    private static String getNonEmptyConfigValue(KeycloakSession session, String key, String description) throws ConfigException {
        verifySession(session);

        String value = "";
        for (AuthenticatorConfigModel config : session.getContext().getRealm().getAuthenticatorConfigs()) {
            if (config.getConfig().containsKey(key)) {
                value = getNonEmptyConfigValue(config, key, description);
            }
        }

        if (value == null || value.isEmpty()) throwHankoConfigException(key, description);
        return value;
    }

    private static String getNullableConfigValue(KeycloakSession session, String key, String description) throws ConfigException {
        verifySession(session);
        for (AuthenticatorConfigModel config : session.getContext().getRealm().getAuthenticatorConfigs()) {
            if (config.getConfig().containsKey(key)) {
                return getNullableConfigValue(config, key, description);
            }
        }
        return null;
    }

    private static void verifySession(KeycloakSession session) throws ConfigException {
        if (session == null) {
            throw new ConfigException("Could not find Hanko apikey because the session is null. " +
                    "There might be a problem with you authentication flow configuration.");
        }

        if (session.getContext() == null) {
            throw new ConfigException("Could not find Hanko apikey because the context is null. " +
                    "There might be a problem with you authentication flow configuration.");
        }

        if (session.getContext().getRealm() == null) {
            throw new ConfigException("Could not find Hanko apikey because the realm is null. " +
                    "There might be a problem with you authentication flow configuration.");
        }
    }

}
