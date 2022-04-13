package io.hanko.plugin.keycloak.config;

import org.keycloak.models.AuthenticatorConfigModel;

public class AuthenticatorConfig extends AbstractConfig<AuthenticatorConfigModel> {

    AuthenticatorConfig(AuthenticatorConfigModel config) {
        super(config);
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


}
