package dk.kofoed.proxy.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kofoed.proxy.client.OidcClient;
import dk.kofoed.proxy.client.model.AccessTokenResponse;
import dk.kofoed.proxy.client.model.OpenIdConfigurationResponse;
import dk.kofoed.proxy.domain.AuthenticationSessionData;
import dk.kofoed.proxy.exception.OidcProviderException;
import dk.kofoed.proxy.exception.ProxyClientException;

@ApplicationScoped
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private OpenIdConfigurationResponse openIdConfiguration;

    @ConfigProperty(name = "auth.proxy.oidc.response.type", defaultValue = "code")
    String responseType;

    @ConfigProperty(name = "auth.proxy.oidc.client.id", defaultValue = "dummy-client-id")
    String clientId;

    @ConfigProperty(name = "auth.proxy.oidc.client.secret", defaultValue = "dummy-client-secret")
    String clientSecret;

    @ConfigProperty(name = "auth.proxy.oidc.redirect.uri", defaultValue = "http://localhost:8080/oidc/callback")
    String redirectUri;

    @ConfigProperty(name = "auth.proxy.oidc.provider.auth.uri", defaultValue = "-")
    String oidcAuthUri;

    @ConfigProperty(name = "auth.proxy.oidc.base.url")
    String oidcBaseUri;

    @Inject
    @RestClient
    OidcClient oidcClient;

    @Inject
    ProofKeyCodeExchangeService pkceService;

    /**
     * Fill in variable parts of the  template (OIDC_AUTH_URI_TEMPLATE environment variable).
     * Add state UUID to list of state IDs. 
     */
    public synchronized String buildAuthInitUri() {
        String stateId = UUID.randomUUID().toString();
        AuthenticationSessionData sessionData = pkceService.buildNewSessionData(stateId);

        String authInitRedirectUrl = openIdConfiguration.authorizationEndpoint() + oidcAuthUri.formatted(
            responseType,
            clientId,
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            sessionData.state(),
            sessionData.codeChallenge()
        );

        return authInitRedirectUrl;
    }

    /**
     * Check if a given state UUID was created as a result of our own authorization code flow.
     * Remove UUID once checked, since IDs are only valid once per flow.
     */
    public boolean stateIdOk(String id) {
        return pkceService.containsState(id);
    }
    
    /**
     * Exhange an auth token with a full access_token by calling OIDC provider token endpoint
     * using <code>authorization_code</code> grant type.
     */
    public AccessTokenResponse getAccessToken(String authCode) {

        logger.info("Getting access_token for auth code [{}]", authCode);

        try {
            return oidcClient.getAccessToken(
                buildTokenEndpoint(openIdConfiguration.tokenEndpoint()),
                "authorization_code",
                clientId,
                clientSecret,
                authCode,
                redirectUri
            );
        } catch (OidcProviderException e) {
            logger.error("Could not retrieve an access token from OIDC provider. Error code: [{}]. Message: [{}]", 
                e.getErrorCode(), 
                e.getMessage()
            );
            throw new ProxyClientException("Could not retrieve access token", 401);
        }
    }

    /**
     * Request a new access_token based on a refresh_token by calling OIDC provider token endpoint
     * using <code>refresh_token</code> grant type.
     */
    public Uni<AccessTokenResponse> refreshAccessToken(String refreshToken) {

        logger.info("Refreshing access_token ...");

        return oidcClient.refreshAccessToken(
            buildTokenEndpoint(openIdConfiguration.tokenEndpoint()),
            "refresh_token",
            clientId,
            clientSecret,
            refreshToken
        ).onFailure(OidcProviderException.class).invoke(failure -> {
            OidcProviderException e = (OidcProviderException) failure;
            logger.error("Could not refresh access token. Error code: [{}]. Message: [{}]", e.getErrorCode(), e.getMessage(), e);
        });
    } 

    public String buildTokenEndpoint(String fullyQualifiedEndpoint) {
        String tokenEndpoint = fullyQualifiedEndpoint.substring(oidcBaseUri.length() + 1);
        logger.info("Token endpoint: [{}]", tokenEndpoint);
        return tokenEndpoint;
    }

    /**
     * Do initial tasks to get up and running.
     */
    @PostConstruct
    public void init() {
        this.openIdConfiguration = oidcClient.getOpenIdConfiguration();
        logger.info("Got OpenID Configuration: [{}]", openIdConfiguration);
    }

}
