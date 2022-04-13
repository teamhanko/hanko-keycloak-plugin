package io.hanko.plugin.keycloak;

import io.hanko.plugin.keycloak.authenticator.MultiAuthenticator;
import io.hanko.plugin.keycloak.config.ConfigException;
import io.hanko.plugin.keycloak.authenticator.MultiAuthenticatorFactory;
import io.hanko.plugin.keycloak.config.Configurable;
import io.hanko.plugin.keycloak.config.ConfigFactory;
import io.hanko.sdk.HankoClient;
import io.hanko.sdk.HankoClientConfig;
import io.hanko.sdk.util.HankoUtils;
import org.keycloak.models.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class HankoClientFactory {

    /**
     * Creates a Hanko Client configured for the specified Keycloak session
     *
     * @param session The Keycloak Session
     * @return HankoClient The HankoClient for the specified Keycloak session
     * @throws ConfigException
     */
    public static HankoClient getHankoClient(KeycloakSession session) throws ConfigException {
        Configurable config = ConfigFactory.getConfig(session);
        HankoClientConfig hankoClientConfig;
        hankoClientConfig = new HankoClientConfig(config.getApiUrl(), config.getApiKeyId(), config.getApiKey());
        return HankoUtils.createHankoClient(hankoClientConfig);
    }

    /**
     * Since the Realm configurations can be different a new Hanko Client is constructed every time based on the user to enroll
     *
     * @param model
     * @return HankoClient The HankoClient for the specifiv realm
     * @throws ConfigException
     */
    public static HankoClient getHankoClient(RealmModel model) throws ConfigException {
        AuthenticatorConfigModel configModel = getHankoAuthenticatorConfig(model);
        Configurable config = ConfigFactory.getConfig(configModel);
        HankoClientConfig hankoClientConfig = new HankoClientConfig(config.getApiUrl(), config.getApiKeyId(), config.getApiKey());
        return HankoUtils.createHankoClient(hankoClientConfig);
    }

    /**
     * Given a RealmModel, finds the first non-disabled {@link MultiAuthenticator
     * HankoMultiAuthenticator} execution for the current bound browser flow and returns its
     * {@link AuthenticatorConfigModel}.
     * <p>
     * <p>
     * Assumes that even if there are multiple HankoMultiAuthenticator executions in the currently bound browser flow,
     * their underlying AuthenticatorConfigModels should be the same (i.e. they should target the same relying party
     * and hence have the same base URL, API Key ID and API Key configured). Having a (browser) flow for a given realm
     * with multiple HankoMultiAuthenticator executions targeting different relying parties is not a feasible use case.
     *
     * @param realm The realm for which the HankoMultiAuthenticator configuration should be determined
     * @return the AuthenticatorConfigModel for the HankoMultiAuthenticator execution in the bound browser flow of the
     * given realm
     * @throws ConfigException if no configuration was found
     */
    private static AuthenticatorConfigModel getHankoAuthenticatorConfig(RealmModel realm) throws ConfigException {
        AuthenticationFlowModel browserFlow = realm.getBrowserFlow();
        List<AuthenticationExecutionModel> topLevelExecutions = realm.getAuthenticationExecutions(browserFlow.getId());
        Queue<AuthenticationExecutionModel> queue = new LinkedList<>(topLevelExecutions);

        while (!queue.isEmpty()) {
            AuthenticationExecutionModel currentExecution = queue.remove();
            String authenticator = currentExecution.getAuthenticator();
            if (currentExecution.isEnabled() && authenticator != null && authenticator.equals(MultiAuthenticatorFactory.ID)) {
                return realm.getAuthenticatorConfigById(currentExecution.getAuthenticatorConfig());
            } else if (currentExecution.isAuthenticatorFlow()) {
                queue.addAll(realm.getAuthenticationExecutions(currentExecution.getFlowId()));
            }
        }

        throw new ConfigException("No authenticator configuration found for bound browser flow");
    }
}
