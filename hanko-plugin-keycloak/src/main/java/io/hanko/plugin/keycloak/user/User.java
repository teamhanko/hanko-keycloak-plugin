package io.hanko.plugin.keycloak.user;

import org.keycloak.models.UserModel;

import java.util.LinkedList;
import java.util.List;

public class User {
    private String CONFIG_REGISTRATION_ID = "HANKO_REGISTRATION_ID";
    private String CONFIG_REQUEST_ID = "HANKO_REQUEST_ID";
    private String CONFIG_ENROLLMENT_UAF_REQUEST_ID = "HANKO_ENROLLMENT_UAF_REQUEST_ID";
    private String CONFIG_ENROLLMENT_WEBAUTHN_REQUEST_ID = "HANKO_ENROLLMENT_WEBAUTHN_REQUEST_ID";

    private UserModel userModel;

    public User(UserModel userModel) {
        this.userModel = userModel;
    }

    public UserModel delegate() {
        return userModel;
    }

    public String getHankoUserId() {
        return getUserAttribute(CONFIG_REGISTRATION_ID);
    }

    public void setHankoUserId(String userId) {
        setUserAttribute(CONFIG_REGISTRATION_ID, userId);
    }

    public String getHankoRequestId() {
        return getUserAttribute(CONFIG_REQUEST_ID);
    }

    public void setHankoRequestId(String requestId) {
        setUserAttribute(CONFIG_REQUEST_ID, requestId);
    }

    public void setEnrollmentUafRequestId(String requestId) {
        setUserAttribute(CONFIG_ENROLLMENT_UAF_REQUEST_ID, requestId);
    }

    public void setEnrollmentWebAuthnRequestId(String requestId) {
        setUserAttribute(CONFIG_ENROLLMENT_WEBAUTHN_REQUEST_ID, requestId);
    }

    private void setUserAttribute(String name, String value) {
        List<String> attributes = new LinkedList<String>();

        if (value != null) {
            attributes.add(value);
        }

        delegate().setAttribute(name, attributes);
    }

    private String getUserAttribute(String name) {
        List<String> registrationId = delegate().getAttribute(name);

        if (registrationId == null || registrationId.isEmpty()) {
            return null;
        }

        return registrationId.get(0);
    }

    @Override
    public String toString() {
        return String.format("%s (ID: %s, Hanko-ID: %s)", super.toString(), delegate().getId(), getHankoUserId());
    }
}
