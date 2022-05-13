package io.hanko.plugin.keycloak.authenticator;

import org.keycloak.credential.CredentialModel;

public class HankoCredentialModel extends CredentialModel {
    public static final String TYPE = "HANKO";

    private final String hankoId;

    public static HankoCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        HankoCredentialModel hankoCredentialModel = new HankoCredentialModel(credentialModel.getCredentialData());
        hankoCredentialModel.setUserLabel(credentialModel.getUserLabel());
        hankoCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
        hankoCredentialModel.setType(TYPE);
        hankoCredentialModel.setId(credentialModel.getId());
        hankoCredentialModel.setSecretData(credentialModel.getSecretData());
        hankoCredentialModel.setCredentialData(credentialModel.getCredentialData());
        return hankoCredentialModel;
    }

    public HankoCredentialModel(String hankoId) {
        this.hankoId = hankoId;
    }

    public String getHankoId() {
        return hankoId;
    }
}
