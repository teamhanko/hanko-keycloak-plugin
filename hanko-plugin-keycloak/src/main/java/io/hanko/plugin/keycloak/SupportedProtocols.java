package io.hanko.plugin.keycloak;

public enum SupportedProtocols {
    UAF(0),
    WEBAUTHN(1);

    private final int value;

    SupportedProtocols(final int value) { this.value = value; }

    public int getValue() { return value; }
}
