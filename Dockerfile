FROM quay.io/keycloak/keycloak:18.0.0

COPY hanko-plugin-keycloak/target/hanko-plugin-keycloak.jar /opt/keycloak/providers/hanko-plugin-keycloak.jar

