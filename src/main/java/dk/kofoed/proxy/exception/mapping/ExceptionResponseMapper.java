package dk.kofoed.proxy.exception.mapping;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import dk.kofoed.proxy.exception.ProxyClientException;

public class ExceptionResponseMapper {

    @Inject
    UriInfo uriInfo;

    /**
     * Map <code>ProxyClientException</code> to an HTTP response.
     */
    @ServerExceptionMapper
    public Response mapException(ProcessingException e) throws InterruptedException {
        if (e.getCause() instanceof ProxyClientException ex) {

            // If we receive a 401 from backend calls, it means that we passed a useless access_token. This happens
            // when we're still trying to refresh the token but OIDC token endpoint reply isn't in yet (async call).
            // When this occurs, we instruct the client to try again after a small delay:
            if (ex.getStatusCode() == 401) {
                Thread.sleep(200);
                return Response.temporaryRedirect(uriInfo.getRequestUri()).build();
            } else {
                return Response.status(ex.getStatusCode()).entity(ex.getResponseBody()).build();
            }
        }
        return Response.serverError().build();
    }

}
