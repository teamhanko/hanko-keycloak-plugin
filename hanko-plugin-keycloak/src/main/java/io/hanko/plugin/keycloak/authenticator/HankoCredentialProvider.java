/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hanko.plugin.keycloak.authenticator;

import org.keycloak.common.util.Time;
import org.keycloak.credential.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HankoCredentialProvider implements CredentialProvider<HankoCredentialModel>, CredentialInputValidator, OnUserCache {
    private KeycloakSession session;

    public HankoCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    private UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        String type = HankoCredentialModel.TYPE;

        if (!supportsCredentialType(credentialType)) {
            return false;
        }

        if (!type.equals(credentialType)) {
            return false;
        }

        return !session.userCredentialManager().getStoredCredentialsByType(realm, user, type).isEmpty();
    }

    @Override
    public String getType() {
        return HankoCredentialModel.TYPE;
    }

    @Override
    public org.keycloak.credential.CredentialModel createCredential(RealmModel realm, UserModel user, HankoCredentialModel credentialModel) {
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }
        return getCredentialStore().createCredential(realm, user, credentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return getCredentialStore().removeStoredCredential(realm, user, credentialId);
    }

    @Override
    public HankoCredentialModel getCredentialFromModel(org.keycloak.credential.CredentialModel model) {
        return HankoCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        throw new UnsupportedOperationException("Authenticator should validate credential");
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return HankoCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext ctx) {
        // TODO Return something meaningful??
        return null;
    }

}
