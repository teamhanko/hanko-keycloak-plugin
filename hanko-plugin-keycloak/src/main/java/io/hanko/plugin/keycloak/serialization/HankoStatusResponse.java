package io.hanko.plugin.keycloak.serialization;

public class HankoStatusResponse {
    public HankoStatusResponse(boolean isPasswordlessActive) {
        this.isPasswordlessActive = isPasswordlessActive;
    }
    public boolean isPasswordlessActive;
}
