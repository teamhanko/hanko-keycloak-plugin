package io.hanko.plugin.keycloak.provider;

import io.hanko.sdk.models.ClientData;
import org.keycloak.models.KeycloakContext;

class ClientDataFactory {
    static ClientData getClientData(KeycloakContext context) {
        String remoteAddress = context.getConnection().getRemoteAddr();
        ClientData clientData = new ClientData();
        clientData.setRemoteAddress(remoteAddress);
        return clientData;
    }
}
