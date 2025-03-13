package dk.kofoed.proxy.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import dk.kofoed.proxy.client.model.AccessTokenResponse;
import dk.kofoed.proxy.client.model.OpenIdConfigurationResponse;
import dk.kofoed.proxy.exception.OidcProviderException;

@RegisterRestClient(configKey = "oidc-provider")
public interface OidcClient {

    @GET
    @Path("/.well-known/openid-configuration")
    @Produces(MediaType.APPLICATION_JSON)
    public OpenIdConfigurationResponse getOpenIdConfiguration();
    
    @POST
    @Path("/{tokenEndpoint}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public AccessTokenResponse getAccessToken(
        @PathParam("tokenEndpoint") @Encoded String tokenEndpoint,
        @FormParam("grant_type") String grantType,
        @FormParam("client_id") String clientId,
        @FormParam("client_secret") String clientSecret,
        @FormParam("code") String authCode,
        @FormParam("code_verifier") String codeVerifier,
        @FormParam("redirect_uri") String redirectUri
    );

    @POST
    @Path("/{tokenEndpoint}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<AccessTokenResponse> refreshAccessToken(
        @PathParam("tokenEndpoint") @Encoded String tokenEndpoint,
        @FormParam("grant_type") String grantType,
        @FormParam("client_id") String clientId,
        @FormParam("client_secret") String clientSecret,
        @FormParam("refresh_token") String refreshToken
    );

    /**
     * Map response error codes to exceptions.
     */
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        String body = "-";
        if (response.hasEntity()) {
            body = response.readEntity(String.class);
        }
        if (response.getStatus() == 400) {
            String message = "Error calling token endpoint. Response: [" + body + "]";
            throw new OidcProviderException(message, response.getStatus());
        }
        if (response.getStatus() == 401) {
            String message = "Access to token endpoint unauthorized. Response: [" + body + "]";
            throw new OidcProviderException(message, response.getStatus());
        }
        return null;
    }
}
