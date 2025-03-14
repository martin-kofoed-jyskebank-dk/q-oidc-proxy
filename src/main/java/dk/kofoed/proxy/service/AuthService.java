package dk.kofoed.proxy.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.quarkus.runtime.Startup;
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

    @ConfigProperty(name = "auth.proxy.oidc.client.id", defaultValue = "dummy-client-id")
    String clientId;

    @ConfigProperty(name = "auth.proxy.oidc.client.secret", defaultValue = "dummy-client-secret")
    String clientSecret;

    @ConfigProperty(name = "auth.proxy.oidc.redirect.uri", defaultValue = "http://localhost:8080/oidc/callback")
    String redirectUri;

    @ConfigProperty(name = "auth.proxy.oidc.provider.auth.uri")
    String oidcAuthUri;

    @ConfigProperty(name = "auth.proxy.oidc.base.url")
    String oidcBaseUrl;

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
        return openIdConfiguration.authorizationEndpoint() + doVarSubstitution(oidcAuthUri, sessionData); 
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
    public AccessTokenResponse getAccessToken(String authCode, String state) {

        String codeVerifier = pkceService.getCodeVerifier(state);

        logger.info("Getting access_token for auth code [{}]. Code verifier: [{}]", authCode, codeVerifier);

        try {
            AccessTokenResponse response = oidcClient.getAccessToken(
                buildTokenEndpoint(openIdConfiguration.tokenEndpoint()),
                "authorization_code",
                clientId,
                clientSecret,
                authCode,
                codeVerifier,
                redirectUri
            );
            pkceService.removeSessionData(state);
            return response;
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

    private String buildTokenEndpoint(String fullyQualifiedEndpoint) {
        return fullyQualifiedEndpoint.substring(oidcBaseUrl.length() + 1);
    }

    private String doVarSubstitution(String authUriTemplate, AuthenticationSessionData sessionData) {
        return authUriTemplate
            .replace("{{redirectUri}}", URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
            .replace("{{codeChallenge}}", sessionData.codeChallenge())
            .replace("{{state}}", sessionData.state())
            .replace("{{clientId}}", clientId);
    }

    /**
     * Load OIDC configuration for selected endpoint. Fail startup if loading fails.
     */
    @Startup
    public void init() {
        try {
            this.openIdConfiguration = oidcClient.getOpenIdConfiguration();
        } catch (Exception e) {
            logger.error(
                "Could not load openid-configuration from .well-known uri for base URL: [{}]. Message: [{}]", 
                oidcBaseUrl, 
                e.getMessage()
            );
            throw new OidcProviderException("Could not load OpenID configuration for base URL [" + oidcBaseUrl + "]", 500);
        }
        if (this.openIdConfiguration.issuer() == null) {
            throw new OidcProviderException("Could not load OpenID configuration for base URL [" + oidcBaseUrl + "]", 500);
        }
        logger.info("Got OpenID Configuration: [{}]", openIdConfiguration);
    }

}
