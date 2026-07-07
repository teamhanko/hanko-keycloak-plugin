FROM quay.io/keycloak/keycloak:26.5.2

COPY hanko-plugin-keycloak/target/hanko-plugin-keycloak-3.0.7-kc26.5.2.jar /opt/keycloak/providers/hanko-plugin-keycloak-3.0.7-kc26.5.2.jar

RUN /opt/keycloak/bin/kc.sh build