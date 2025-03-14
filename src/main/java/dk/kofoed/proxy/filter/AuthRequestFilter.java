package dk.kofoed.proxy.filter;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kofoed.proxy.api.model.RedirectDataResponse;
import dk.kofoed.proxy.client.model.AccessTokenResponse;
import dk.kofoed.proxy.domain.AuthType;
import dk.kofoed.proxy.exception.TokenExpiredException;
import dk.kofoed.proxy.exception.TokenNotFoundException;
import dk.kofoed.proxy.service.AuthService;
import dk.kofoed.proxy.service.TokenCache;
import dk.kofoed.proxy.service.TokenHelper;

public class AuthRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthRequestFilter.class);

    @ConfigProperty(name = "auth.proxy.type")
    AuthType authType;

    @ConfigProperty(name = "auth.proxy.oidc.base.url")
    String oidcProviderBaseUri;

    @ConfigProperty(name = "auth.proxy.header.name")
    String headerName;

    @ConfigProperty(name = "auth.proxy.cookie.name")
    String cookieName;

    @Inject
    AuthService authService;

    @Inject
    TokenCache tokenCache;

    @Inject
    TokenHelper tokenHelper;

    /**
     * Check authentication before endpoint url pattern matching. 
     */
    @ServerRequestFilter(preMatching = true)
    public Optional<Response> authFilter(ContainerRequestContext context) {

        String path = context.getUriInfo().getPath();
        
        if (path.equals("/oidc/callback") || path.equals("/oidc/authcode") || path.startsWith("/q/health")) {
            // oidc callback requests should pass this filter without further checks
            return Optional.empty();
        } 
        
        if (!path.startsWith("/api")) {
            return badRequest("Auth proxy calls must be prefixed by /api");
        }
        
        logger.info("Path: [{}]", path);

        // check incoming auth code based on configured authentication type
        String authCode = null;
        if (authType == AuthType.BEARER) {
            authCode = context.getHeaderString(headerName);
            if (authCode != null && authCode.startsWith("Bearer ")) {
                authCode = authCode.substring(7);
            } 
        }
        if (authType == AuthType.COOKIE) {
            Map<String, Cookie> cookies = context.getCookies();
            if (cookies.containsKey(cookieName)) {
                authCode = cookies.get(cookieName).getValue();
            } 
        }

        if (authCode == null) {
            logger.info("No auth code found for auth type [{}]. Path = [{}]. Sending 401 with redirect data.", authType, path);
            return unauth();
        } else {

            final String cacheKey = authCode;
            
            try {

                AccessTokenResponse cachedToken = tokenCache.get(cacheKey);

                // if we have a refresh_token available, check if it is expired. If so, return immediately
                if (cachedToken.refreshToken() != null) {
                    if (tokenHelper.tokenExpired(cachedToken.refreshToken(), 0)) {
                        logger.info("Token found but refresh_token expired. Sending redirect info.");
                        tokenCache.remove(cacheKey);
                        return unauth();
                    }
                }

                checkTokenRefresh(cacheKey);

                context.getHeaders().remove(headerName);
                context.getHeaders().add(headerName, "Bearer " + cachedToken.accessToken());
            } catch (TokenNotFoundException e) {
                logger.warn("No valid token found for authorization code");
                return unauth();
            }
        }
        return Optional.empty();
    }

    /**
     * Check if access_token needs to be refreshed. If it does, make an async call to 
     * oidc token endpoint using refresh_token.
     * @throws TokenNotFoundException if cache does not contain any entries for specified key
     */
    private void checkTokenRefresh(String cacheKey) throws TokenNotFoundException {
        tokenCache
            .checkRefresh(cacheKey)
            .onFailure(TokenExpiredException.class)
            .recoverWithUni(failure -> {
                TokenExpiredException e = (TokenExpiredException) failure;
                return authService.refreshAccessToken(e.getRefreshToken())
                    .onFailure().retry()
                    .withBackOff(Duration.ofMillis(50), Duration.ofSeconds(1))
                    .atMost(5)
                    .invoke(token -> {
                        tokenCache.put(cacheKey, token);
                    });
            }).subscribe().with(
                token -> {
                    // do not do anything with token here, as it is either being refreshed or already in the cache ...
                },
                failure -> {
                    logger.warn("Could not get or refresh access_token. Refresh token may be expired. Message: [{}]", 
                        failure.getMessage());
                    tokenCache.remove(cacheKey);
                }
            );
    }

    private Optional<Response> unauth() {
        URI oidcProvider = null;
        String url = "";
        try {
            url = authService.buildAuthInitUri();
            oidcProvider = new URI(url);
        } catch (URISyntaxException e) {
            return badRequest("Could not parse OIDC Provider URL: " + url);
        }
        RedirectDataResponse redirectResponse = new RedirectDataResponse(oidcProvider.toString());
        return Optional.of(Response.status(Status.UNAUTHORIZED).entity(redirectResponse).build());
    }

    private Optional<Response> badRequest(String message) {
        String error = """
                {
                    "error": "%s"
                }
                """.formatted(message);
                
        return Optional.of(Response.status(Status.BAD_REQUEST).entity(error).build());
    }
    
}
