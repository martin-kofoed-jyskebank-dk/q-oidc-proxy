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

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import dk.kofoed.proxy.client.ProxyClient;
import dk.kofoed.proxy.client.RestPathHelper;

@RequestScoped
@Path("/api")
public class ProxyApi {

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
    public Uni<String> proxyBackendGetData(@PathParam("any") String proxyPath) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendGet();
    }

    /**
     * Match any paths for POST requests.
     */
    @Path("/{any:.*}")
    @POST
    @Produces(MediaType.WILDCARD)
    @Consumes(MediaType.WILDCARD)
    public Uni<String> proxyBackendPostData(@PathParam("any") String proxyPath, String payload) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendPost(payload);
    }

    /**
     * Match any paths for POST requests.
     */
    @Path("/{any:.*}")
    @PUT
    @Produces(MediaType.WILDCARD)
    @Consumes(MediaType.WILDCARD)
    public Uni<String> proxyBackendPutData(@PathParam("any") String proxyPath, String payload) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendPut(payload);
    }

    /**
     * Match any paths for POST requests.
     */
    @Path("/{any:.*}")
    @PATCH
    @Produces(MediaType.WILDCARD)
    @Consumes(MediaType.WILDCARD)
    public Uni<String> proxyBackendPatchData(@PathParam("any") String proxyPath, String payload) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendPatch(payload);
    }

    /**
     * Match any paths for DELETE requests.
     */
    @Path("/{any:.*}")
    @DELETE
    @Produces(MediaType.WILDCARD)
    @Consumes(MediaType.WILDCARD)
    public Uni<String> proxyBackendDeleteData(@PathParam("any") String proxyPath, String payload) {

        pathHelper.setUri(proxyPath);

        return proxyClient.callBackendDelete(payload);
    }
    
}
