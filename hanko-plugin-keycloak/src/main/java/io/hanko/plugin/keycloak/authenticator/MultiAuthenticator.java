package io.hanko.plugin.keycloak.authenticator;

import io.hanko.plugin.keycloak.HankoLogger;
import io.hanko.plugin.keycloak.SupportedProtocols;
import io.hanko.plugin.keycloak.user.User;
import io.hanko.sdk.HankoClient;
import io.hanko.sdk.models.*;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CookieHelper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MultiAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {
    private final HankoClient hankoClient;

    public enum LoginMethod {EMPTY, UAF, WEBAUTHN} //ROAMING_AUTHENTICATOR, PLATFORM_AUTHENTICATOR}

    MultiAuthenticator(HankoClient hankoClient) {
        this.hankoClient = hankoClient;
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
        MultivaluedMap<String, String> formData = authenticationFlowContext.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("cancel")) {
            authenticationFlowContext.resetFlow();
            return;
        }

        if (formData.containsKey("switch")) {
            LoginMethod loginMethod = LoginMethod.valueOf(formData.getFirst("loginMethod"));
            renderChallenge(loginMethod, authenticationFlowContext, null);
            return;
        }

        User user = new User(authenticationFlowContext.getUser());

        String hankoRequestId = user.getHankoRequestId();

        LoginMethod loginMethod = LoginMethod.valueOf(formData.getFirst("loginMethod"));

        switch (loginMethod) {
            case UAF:
                try {
                    HankoRequest hankoRequest = hankoClient.getUafRequest(hankoRequestId);
                    if (hankoRequest.getStatus() == Status.OK) {
                        setAuthMethodCookie(loginMethod, authenticationFlowContext);
                        authenticationFlowContext.success();
                    } else {
                        HankoLogger.authenticationFailed(SupportedProtocols.UAF, hankoRequestId, user);
                        cancelLogin(authenticationFlowContext);
                    }
                } catch (Exception ex) {
                    HankoLogger.verificationFailed(SupportedProtocols.UAF, hankoRequestId, user, ex);
                    cancelLogin(authenticationFlowContext);
                    authenticationFlowContext.failure(AuthenticationFlowError.INTERNAL_ERROR);
                }

                break;

            case WEBAUTHN:
                HankoLogger.initLogin();
                try {
                    // blocking call to Hanko API
                    String hankoResponse = formData.getFirst("hankoresponse");

                    // get Hanko API key from the Hanko UAF Authenticator configuration
                    HankoRequest hankoRequest = hankoClient.validateWebAuthnRequest(hankoRequestId, hankoResponse);

                    if (hankoRequest.getStatus() == Status.OK) {
                        setAuthMethodCookie(loginMethod, authenticationFlowContext);
                        authenticationFlowContext.success();
                    } else {
                        HankoLogger.authenticationFailed(SupportedProtocols.WEBAUTHN, hankoRequestId, user);
                        authenticationFlowContext.getEvent().user(user.delegate());
                        authenticationFlowContext.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
                        HankoLogger.invalidUserCredentials(user);
                        renderChallenge(LoginMethod.WEBAUTHN, authenticationFlowContext, AuthenticationFlowError.INVALID_CREDENTIALS);
                    }
                } catch (Exception ex) {
                    HankoLogger.verificationFailed(SupportedProtocols.WEBAUTHN, hankoRequestId, user, ex);
                    cancelLogin(authenticationFlowContext);
                    authenticationFlowContext.failure(AuthenticationFlowError.INTERNAL_ERROR);
                }
                break;

            default:
                //TODO: evaluate this path
                authenticationFlowContext.getEvent().user(user.delegate());
                authenticationFlowContext.getEvent().error("Unknown login method. Must be either UAF or WEBAUTHN.");
                renderChallenge(LoginMethod.WEBAUTHN, authenticationFlowContext, AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED);
                break;
        }
    }

    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        List<Device> devices = getDevices(authenticationFlowContext);
        LoginMethod loginMethod = getLoginMethod(authenticationFlowContext, devices);
        renderChallenge(loginMethod, authenticationFlowContext, null);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    private void setAuthMethodCookie(LoginMethod loginMethod, AuthenticationFlowContext authenticationFlowContext) {
        URI uri = authenticationFlowContext.getUriInfo().getBaseUriBuilder().path("realms").path(authenticationFlowContext.getRealm().getName()).build();
        int maxCookieAge = 60 * 60 * 24 * 365; // 365 days
        CookieHelper.addCookie("LOGIN_METHOD", loginMethod.name(), uri.getRawPath(), null, null,
                maxCookieAge, false, true);
    }

    private void cancelLogin(AuthenticationFlowContext context) {
        context.clearUser();
        context.resetFlow();
    }

    private List<Device> getDevices(AuthenticationFlowContext context) {
        List<Device> devices = new LinkedList<>();
        User user = new User(context.getUser());
        String userId = user.getHankoUserId();

        if (!(userId == null || userId.trim().isEmpty())) {
            try {
                devices = Arrays.asList(hankoClient.getRegisteredDevices(userId));
            } catch (Exception ex) {
                HankoLogger.getRegisteredDevicesFailed(user, ex);
            }
        }

        return devices;
    }

    private boolean hasUaf(List<Device> devices) {
        return devices.stream().anyMatch(device -> Objects.equals(device.getAuthenticatorType(), "FIDO_UAF"));
    }

    private boolean hasWebAuthn(List<Device> devices) {
        return devices.stream().anyMatch(device -> Objects.equals(device.getAuthenticatorType(), "WEBAUTHN"));
    }

    private LoginMethod getLoginMethod(AuthenticationFlowContext context, List<Device> devices) {
        User user = new User(context.getUser());
        String hankoUserId = user.getHankoUserId();

        boolean preferWebauthn = CookieHelper.getCookieValue("LOGIN_METHOD").contains(LoginMethod.WEBAUTHN.name());
        boolean preferUaf = CookieHelper.getCookieValue("LOGIN_METHOD").contains(LoginMethod.UAF.name());
        //boolean preferPassword = CookieHelper.getCookieValue("LOGIN_METHOD").contains(LoginMethod.PASSWORD.name());

        if (hankoUserId == null || hankoUserId.trim().isEmpty()) {
            return LoginMethod.EMPTY;
        } else {

            boolean hasWebAuthnAuthenticator = hasWebAuthn(devices);
            boolean hasUafAuthenticator = hasUaf(devices);

            if (preferWebauthn && hasWebAuthnAuthenticator) {
                return LoginMethod.WEBAUTHN;
            } else if (preferUaf && hasUafAuthenticator) {
                return LoginMethod.UAF;
            } else if (hasWebAuthnAuthenticator) {
                return LoginMethod.WEBAUTHN;
            } else if (hasUafAuthenticator) {
                return LoginMethod.UAF;
            } else {
                return LoginMethod.EMPTY;
            }
        }
    }

    private void renderChallenge(AuthenticationFlowContext context, Response response, AuthenticationFlowError error) {
        if (error != null) {
            context.failureChallenge(error, response);
        } else {
            context.challenge(response);
        }
    }

    private void renderChallenge(LoginMethod loginMethod, AuthenticationFlowContext context, AuthenticationFlowError error) {
        User user = new User(context.getUser());

        List<Device> devices = getDevices(context);
        String userId = user.getHankoUserId();
        String username = user.delegate().getUsername();
        String remoteAddress = context.getConnection().getRemoteAddr();

        ClientData clientData = new ClientData();
        clientData.setRemoteAddress(remoteAddress);

        LoginFormsProvider formsProvider = context.form();
        formsProvider.setAttribute("hasUaf", hasUaf(devices));
        formsProvider.setAttribute("hasWebAuthn", hasWebAuthn(devices));
        formsProvider.setAttribute("hasLoginMethods", loginMethod.equals(LoginMethod.WEBAUTHN) && hasUaf(devices) || loginMethod.equals(LoginMethod.UAF) && hasWebAuthn(devices));
        formsProvider.setAttribute("username", username);

        if (error != null) {
            formsProvider.setError(Messages.INVALID_USER);
        }

        switch (loginMethod) {
            case UAF:
                CreateUafRequest createHankoRequest = new CreateUafRequest();
                createHankoRequest.setUserId(userId);
                createHankoRequest.setUsername(username);
                createHankoRequest.setOperation(Operation.AUTH);

                try {
                    HankoRequest hankoRequest = hankoClient.requestUafOperation(createHankoRequest);
                    String requestId = hankoRequest.getId();
                    user.setHankoRequestId(requestId);
                    Response response = formsProvider.setAttribute("requestId", requestId).setAttribute("loginMethod", "UAF").createForm("hanko-multi-login.ftl");
                    renderChallenge(context, response, error);
                } catch (Exception ex) {
                    HankoLogger.registrationFailed(SupportedProtocols.UAF, user, ex);
                    cancelLogin(context);
                }

                break;

            case WEBAUTHN:
                CreateWebAuthnRequest createWebAuthnRequest = new CreateWebAuthnRequest();
                createWebAuthnRequest.setUserId(userId);
                createWebAuthnRequest.setUsername(username);
                createWebAuthnRequest.setOperation(Operation.AUTH);
                AuthenticatorSelectionCriteria authenticatorSelectionCriteria = new AuthenticatorSelectionCriteria();
                authenticatorSelectionCriteria.setUserVerification(UserVerification.REQUIRED);
                createWebAuthnRequest.setAuthenticatorSelectionCriteria(authenticatorSelectionCriteria);

                try {
                    HankoRequest hankoRequest = hankoClient.requestWebAuthnOperation(createWebAuthnRequest);
                    String requestId = hankoRequest.getId();
                    String request = hankoRequest.getRequest();
                    user.setHankoRequestId(requestId);
                    Response response = formsProvider.setAttribute("request", request).setAttribute("loginMethod", "WEBAUTHN").createForm("hanko-multi-login.ftl");
                    renderChallenge(context, response, error);
                } catch (Exception ex) {
                    HankoLogger.registrationFailed(SupportedProtocols.WEBAUTHN, user, ex);
                    cancelLogin(context);
                }

                break;

            default:
                Response response = formsProvider.setAttribute("loginMethod", "PASSWORD").createForm("hanko-multi-login.ftl");
                renderChallenge(context, response, error);
                break;
        }
    }
}
