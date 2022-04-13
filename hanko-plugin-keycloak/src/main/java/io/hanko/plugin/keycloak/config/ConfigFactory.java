package io.hanko.plugin.keycloak.config;

import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;

public class ConfigFactory {

    public static Configurable getConfig(KeycloakSession keycloakSession) {
        return new KeycloakConfig(keycloakSession);
    }

    public static Configurable getConfig(AuthenticatorConfigModel authenticatorConfigModel) {
        return new AuthenticatorConfig(authenticatorConfigModel);
    }

}
