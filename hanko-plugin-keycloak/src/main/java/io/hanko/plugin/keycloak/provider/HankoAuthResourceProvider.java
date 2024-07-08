package io.hanko.plugin.keycloak.provider;

import io.hanko.plugin.keycloak.HankoClientFactory;
import io.hanko.sdk.models.HankoRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class HankoAuthResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public HankoAuthResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("requests/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus(@PathParam("requestId") String requestId) {
        try {
            HankoRequest hankoRequest = HankoClientFactory.getHankoClient(session).getUafRequest(requestId);
            String response = "{\"status\": \"" + hankoRequest.getStatus() + "\"}";
            return Response.ok(hankoRequest).entity(response).build();
        } catch (Exception ex) {
            return Response.serverError().entity("Error getting Hanko request status :" + ex.getMessage()).build();
        }
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }
}
