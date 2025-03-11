package dk.kofoed.proxy.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kofoed.proxy.client.OidcClient;
import dk.kofoed.proxy.client.model.AccessTokenResponse;
import dk.kofoed.proxy.exception.OidcProviderException;

@ApplicationScoped
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private List<String> stateIds;

    @ConfigProperty(name = "auth.proxy.oidc.response.type", defaultValue = "code")
    String responseType;

    @ConfigProperty(name = "auth.proxy.oidc.client.id", defaultValue = "dummy-client-id")
    String clientId;

    @ConfigProperty(name = "auth.proxy.oidc.client.secret", defaultValue = "dummy-client-secret")
    String clientSecret;

    @ConfigProperty(name = "auth.proxy.oidc.redirect.uri", defaultValue = "http://localhost:8080/oidc/callback")
    String redirectUri;

    @ConfigProperty(name = "auth.proxy.oidc.provider.auth.uri", defaultValue = "http://localhost:8080")
    String oidcAuthUri;

    @Inject
    @RestClient
    OidcClient oidcClient;

    /**
     * Fill in variable parts of the  template (OIDC_AUTH_URI_TEMPLATE environment variable).
     * Add state UUID to list of state IDs. 
     */
    public synchronized String buildAuthInitUri() {
        String stateId = UUID.randomUUID().toString();
        stateIds.add(stateId);
        return oidcAuthUri.formatted(
            responseType,
            clientId,
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            stateId
        );
    }

    /**
     * Check if a given state UUID was created as a result of our own authorization code flow.
     * Remove UUID once checked, since IDs are only valid once per flow.
     */
    public boolean stateIdOk(String id) {
        if (this.stateIds.contains(id)) {
            this.stateIds.remove(id);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Exhange an auth token with a full access_token by calling OIDC provider token endpoint
     * using <code>authorization_code</code> grant type.
     */
    public AccessTokenResponse getAccessToken(String authCode) {

        logger.info("Getting access_token ...");

        try {
            return oidcClient.getAccessToken(
                "authorization_code",
                clientId,
                clientSecret,
                authCode,
                redirectUri
            );
        } catch (OidcProviderException e) {
            logger.error("Could not retrieve an access token from OIDC provider. Error code: [{}]. Message: [{}]", 
                e.getErrorCode(), 
                e.getMessage(),
                e
            );
        }
        return null;
    }

    /**
     * Request a new access_token based on a refresh_token by calling OIDC provider token endpoint
     * using <code>refresh_token</code> grant type.
     */
    public Uni<AccessTokenResponse> refreshAccessToken(String refreshToken) {

        logger.info("Refreshing access_token ...");

        return oidcClient.refreshAccessToken(
            "refresh_token",
            clientId,
            clientSecret,
            refreshToken
        ).onFailure(OidcProviderException.class).invoke(failure -> {
            OidcProviderException e = (OidcProviderException) failure;
            logger.error("Could not refresh access token. Error code: [{}]. Message: [{}]", e.getErrorCode(), e.getMessage(), e);
        });
    } 

    @PostConstruct
    public void init() {
        this.stateIds = Collections.synchronizedList(new LinkedList<>());
    }

}
