package dk.kofoed.proxy.filter;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kofoed.proxy.client.RestPathHelper;
import dk.kofoed.proxy.exception.ProxyClientException;

@Provider
public class ClientUriFilter implements ClientRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ClientUriFilter.class);

    @Inject
    RestPathHelper pathHelper;

    @ConfigProperty(name = "jb.auth.backend.base.url")
    String backendBaseUrl;

    /**
     * Ensure that /api is being removed from calls to backend resource by replacing 
     * uri on request context with the one injected via <code>PathHelper</Code>.
     */
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        if (pathHelper.getUri() != null && requestContext.getUri().toString().startsWith(backendBaseUrl)) {
            try {
                URI uriWithPath = new URI(requestContext.getUri().toString() + pathHelper.getUri());
                logger.info("Calling backend resource: [{}]", uriWithPath);
                requestContext.setUri(uriWithPath);
            } catch (URISyntaxException e) {
                logger.error("Could not create uri: [{}{}]", requestContext.getUri(), pathHelper.getUri());
                throw new ProxyClientException("Invalid uri on request to proxy API", Status.BAD_REQUEST.getStatusCode());
            }
        } 
    }
    
}
