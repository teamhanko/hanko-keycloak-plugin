FROM quay.io/keycloak/keycloak:21.1.1

COPY hanko-plugin-keycloak/target/hanko-plugin-keycloak.jar /opt/keycloak/providers/hanko-plugin-keycloak.jar

