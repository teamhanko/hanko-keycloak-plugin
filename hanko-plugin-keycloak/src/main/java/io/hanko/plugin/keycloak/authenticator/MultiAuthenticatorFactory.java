package io.hanko.plugin.keycloak.authenticator;

import io.hanko.plugin.keycloak.HankoClientFactory;
import io.hanko.plugin.keycloak.HankoLogger;
import io.hanko.plugin.keycloak.config.ConfigException;
import io.hanko.sdk.HankoClient;
import io.hanko.sdk.HankoClientConfig;
import io.hanko.sdk.util.HankoUtils;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class MultiAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {
    public static final String ID = "hanko-multi-authenticator";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        try {
            HankoClient hankoClient = HankoClientFactory.getHankoClient(session);
            return new MultiAuthenticator(hankoClient);
        } catch (ConfigException e) {
            HankoLogger.clientConfigFailed();
            HankoClientConfig dummyConfig = new HankoClientConfig("", "", "");
            HankoClient hankoClient = HankoUtils.createHankoClient(dummyConfig);
            return new MultiAuthenticator(hankoClient);
        }
    }

    @Override
    public String getDisplayType() {
        return "Hanko Multi Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return "Hanko";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Hanko Multi Authenticator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName("hanko.apiurl");
        property.setLabel("Hanko API URL");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("https://api.hanko.io");
        property.setHelpText("Please use \"https://api.hanko.io\".");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("hanko.apikeyid");
        property.setLabel("Hanko API KEY ID");
        property.setType(ProviderConfigProperty.PASSWORD);
        property.setHelpText("Hanko API KEY ID.");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("hanko.apikey");
        property.setLabel("Hanko API KEY");
        property.setType(ProviderConfigProperty.PASSWORD);
        property.setHelpText("Hanko API KEY.");
        configProperties.add(property);
    }
}
