package io.hanko.plugin.keycloak.config;

public interface Configurable {
    String getApiUrl() throws ConfigException;

    String getApiKey() throws ConfigException;

    String getApiKeyId() throws ConfigException;
}
