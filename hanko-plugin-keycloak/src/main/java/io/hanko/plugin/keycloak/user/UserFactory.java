package io.hanko.plugin.keycloak.user;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.NotAuthorizedException;

public class UserFactory {
    private static User getUser(AuthenticationManager.AuthResult auth) {
        UserModel user = auth.getUser();
        if (user == null) throw new NotAuthorizedException("Failed to get user from AuthResult");
        return new User(user);
    }

    public static User getUser(KeycloakSession session, String userId, RealmModel realm) {
        if (session == null) throw new NotAuthorizedException("Session cannot be null");
        UserModel user = session.users().getUserById(realm, userId);
        return new User(user);
    }

    public static User getUser(KeycloakSession session) {
        if (session == null) throw new NotAuthorizedException("Session cannot be null");
        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        if (authResult == null) throw new NotAuthorizedException("AuthResult cannot be null");
        return getUser(authResult);
    }
}
