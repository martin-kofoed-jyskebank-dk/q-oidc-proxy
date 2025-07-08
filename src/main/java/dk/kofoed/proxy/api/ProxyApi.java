package dk.kofoed.proxy.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Set;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import dk.kofoed.proxy.client.ProxyClient;
import dk.kofoed.proxy.client.RestPathHelper;

@RequestScoped
@Path("/api")
public class ProxyApi {

    private static final Set<String> HEADER_PROPAGATION_BLACKLIST = Set.of("content-length", "transfer-encoding", "connection");

    @Inject
    @RestClient
    ProxyClient proxyClient;

    @Inject
    RestPathHelper pathHelper;

    /**
     * Match any paths for GET requests.
     */
    @Path("/{any:.*}")
    @GET
    @Produces(MediaType.WILDCARD)
    public Uni<Response> proxyBackendGetData(@PathParam("any") String proxyPath) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendGet().map(this::propagateHeaders);
    }

    /**
     * Match any paths for POST requests.
     */
    @Path("/{any:.*}")
    @POST
    @Produces(MediaType.WILDCARD)
    @Consumes(MediaType.WILDCARD)
    public Uni<Response> proxyBackendPostData(@PathParam("any") String proxyPath, String payload) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendPost(payload).map(this::propagateHeaders);
    }

    /**
     * Match any paths for POST requests.
     */
    @Path("/{any:.*}")
    @PUT
    @Produces(MediaType.WILDCARD)
    @Consumes(MediaType.WILDCARD)
    public Uni<Response> proxyBackendPutData(@PathParam("any") String proxyPath, String payload) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendPut(payload).map(this::propagateHeaders);
    }

    /**
     * Match any paths for POST requests.
     */
    @Path("/{any:.*}")
    @PATCH
    @Produces(MediaType.WILDCARD)
    @Consumes(MediaType.WILDCARD)
    public Uni<Response> proxyBackendPatchData(@PathParam("any") String proxyPath, String payload) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendPatch(payload).map(this::propagateHeaders);
    }

    /**
     * Match any paths for DELETE requests.
     */
    @Path("/{any:.*}")
    @DELETE
    @Produces(MediaType.WILDCARD)
    @Consumes(MediaType.WILDCARD)
    public Uni<Response> proxyBackendDeleteData(@PathParam("any") String proxyPath, String payload) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendDelete(payload).map(this::propagateHeaders);
    }

    /**
     * Grab headers from original client response and add to new response unless blacklisted.
     */
    private Response propagateHeaders(Response originalResponse) {

        Response.ResponseBuilder responseBuilder = Response
            .status(originalResponse.getStatus())
            .entity(originalResponse.getEntity());

        originalResponse.getHeaders().keySet().forEach(key -> {
            if (!HEADER_PROPAGATION_BLACKLIST.contains(key.toLowerCase())) {
                responseBuilder.header(key, originalResponse.getHeaderString(key));
            }
        });
        return responseBuilder.build();
    }    
    
}
