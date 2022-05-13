package io.hanko.plugin.keycloak.config;

import org.keycloak.models.AuthenticatorConfigModel;

abstract class AbstractConfig<T> implements Configurable {

    static String CONFIG_API_URL = "hanko.apiurl";
    static String CONFIG_APIKEY = "hanko.apikey";
    static String CONFIG_APIKEYID = "hanko.apikeyid";

    T source;

    AbstractConfig(T source) {
        this.source = source;
    }

    static String getNonEmptyConfigValue(AuthenticatorConfigModel config, String key, String description) throws ConfigException {
        String value = config.getConfig().get(key);
        if (key == null || key.trim().isEmpty()) {
            throwHankoConfigException(key, description);
        }
        return value;
    }

    static String getNullableConfigValue(AuthenticatorConfigModel config, String key, String description) throws ConfigException {
        return config.getConfig().get(key);
    }

    static void throwHankoConfigException(String key, String description) throws ConfigException {
        throw new ConfigException("Could not find " + key + " (" +  description + "). " +
                "Please set its value in the configuration for the Hanko Multi Authenticator " +
                "in your authentication flow.");
    }
}
