package io.hanko.plugin.keycloak;

import io.hanko.plugin.keycloak.user.User;
import org.keycloak.services.ServicesLogger;

public class HankoLogger {
    private static ServicesLogger LOGGER = ServicesLogger.LOGGER;

    static public void clientConfigFailed() {
        LOGGER.error("HANKO00001: Configuration Exception for HankoMultiAuthenticator. The Hanko Client will not function properly.");
    }

    static public void setAttributeFailed(String attr, Throwable t) {
        LOGGER.errorf("HANKO00002: Failed to set attribute - attribute: %s, message: %s", attr, t);
    }

    static public void processTemplateFailed(String templateName, Throwable t) {
        LOGGER.errorf("HANKO00003: Failed to process template - templateName: %s, message: %s", templateName, t);
    }

    static public void loadThemeFailed(Throwable t) {
        LOGGER.errorf("HANKO00004: Failed to load theme - message: %s", t);
    }

    static public void authenticationFailed(SupportedProtocols protocol, String requestId, User user) {
        LOGGER.warnf("HANKO0005: %s authentication failed - requestId: %s, user: %s", protocol.name(), requestId, user);
    }

    static public void verificationFailed(SupportedProtocols protocol, String requestId, User user, Throwable t) {
        LOGGER.warnf("HANKO0006: %s verification failed - requestId: %s, user: %s, message: %s", protocol.name(), requestId, user, t);
    }

    static public void registrationFailed(SupportedProtocols protocol, User user, Throwable t) {
        LOGGER.warnf("HANKO0007: Failed to request %s registration - user: %s, message: %s", protocol.name(), user, t);
    }

    static public void deregistrationFailed(SupportedProtocols protocol, User user, String deviceId, Throwable t) {
        LOGGER.warnf("HANKO0008: Failed to request %s registration - user: %s, deviceId: %s, message: %s", protocol.name(), user, deviceId, t);
    }

    static public void uafCancelRequestFailed(User user, Throwable t) {
        LOGGER.warnf("HANKO0009: Failed to request UAF de-registration - user: %s, message: %s", user, t);
    }

    static public void renameDeviceFailed(User user, String deviceId, Throwable t) {
        LOGGER.warnf("HANKO0011: Failed to rename device - user: %s, device: %s, message: %s", user, deviceId, t);
    }

    static public void getRegisteredDevicesFailed(User user, Throwable t) {
        LOGGER.warnf("HANKO0012: Failed to get registered devices - user: %s, message: %s", user, t);
    }

    static public void invalidUserCredentials(User user) {
        LOGGER.warnf("HANKO0013: Login attempt with invalid user credentials - user: %s", user);
    }

    static public void initLogin() {
        LOGGER.warn("HANKO0013: init");
    }

}
