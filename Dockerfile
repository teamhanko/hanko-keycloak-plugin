FROM jboss/keycloak:15.0.2

COPY hanko-plugin-keycloak-ear/target/hanko-plugin-keycloak.ear /opt/jboss/keycloak/standalone/deployments/hanko-plugin-keycloak.ear

