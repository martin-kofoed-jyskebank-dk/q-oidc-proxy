package dk.kofoed.proxy.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import dk.kofoed.proxy.exception.ProxyClientException;
import dk.kofoed.proxy.filter.ClientUriFilter;

@RegisterRestClient(configKey = "proxy")
@RegisterClientHeaders
@RegisterProvider(ClientUriFilter.class)
@Path("")
public interface ProxyClient {
    
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Uni<Response> callBackendGet();

    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public Uni<Response> callBackendPost(String payload);

    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    public Uni<Response> callBackendPut(String payload);

    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PATCH
    public Uni<Response> callBackendPatch(String payload);

    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @DELETE
    public Uni<Response> callBackendDelete(String payload);

    /**
     * Map error response to <code>ProxyClientException</code>. If error response has a payload, 
     * we make sure to add this to the exception.
     */
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        String body = "{}";
        if (response.hasEntity()) {
            body = response.readEntity(String.class);
        }
        if (response.getStatus() >= 400) {
            String message = "Error proxied backend ressource. Status: [" + response.getStatus() + "]. Response: [" + body + "]";
            throw new ProxyClientException(message, body, response.getStatus());
        }
        return null;
    }

}
