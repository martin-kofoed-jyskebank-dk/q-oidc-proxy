package dk.kofoed.proxy.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.NewCookie.SameSite;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kofoed.proxy.client.model.AccessTokenResponse;
import dk.kofoed.proxy.domain.AuthType;
import dk.kofoed.proxy.exception.StateMismatchException;
import dk.kofoed.proxy.service.AuthService;
import dk.kofoed.proxy.service.TokenCache;

@RequestScoped
@Path("/oidc")
public class OidcApi {

    private static final Logger logger = LoggerFactory.getLogger(OidcApi.class);

    @ConfigProperty(name = "auth.proxy.type")
    AuthType authType;

    @ConfigProperty(name = "auth.proxy.cookie.name")
    String cookieName;

    @ConfigProperty(name = "auth.proxy.frontend.redirect")
    String frontendRedirect;

    @ConfigProperty(name = "auth.proxy.cookie.domain")
    String cookieDomain;

    @ConfigProperty(name = "auth.proxy.frontend.callback.param.name")
    String callbackParamName;

    @ConfigProperty(name = "auth.proxy.cookie.samesite")
    SameSite sameSite;

    @Inject
    AuthService authService;

    @Inject
    TokenCache tokenCache;

    /**
     * Handle OAuth callbacks from OIDC provider.
     */
    @Path("/callback")
    @GET
    public Response oauthCallback(
        @QueryParam("state") String state,
        @QueryParam("session_state") String sessionState,
        @QueryParam("code") String authCode) throws URISyntaxException {
        
        logger.info("Got callback from OIDC provider. State: [{}]. Session state: [{}]. Authorization code: [{}].", 
            state, 
            sessionState, 
            authCode
        );

        // check if state UUID is one issued by us, otherwise throw exception:
        if (!authService.stateIdOk(state)) {
            throw new StateMismatchException("State UUID returned from OIDC provider did not match any created by us");
        }

        // Exchange auth token for a full access token / JWT and add to cache
        AccessTokenResponse accessToken = authService.getAccessToken(authCode);
        if (accessToken == null) {
            return Response.serverError().build();
        } else {
            tokenCache.put(authCode, accessToken);
        }

        // depending on which auth type is selected, return authentication token to client
        if (authType == AuthType.COOKIE) {
            NewCookie cookie = getCookie(authCode);
            return Response.status(Status.FOUND).location(new URI(frontendRedirect)).cookie(cookie).build();
        }
        if (authType == AuthType.BEARER) {
            return Response.status(Status.FOUND).location(new URI(buildRedirect(authCode))).build();
        }

        return Response.seeOther(new URI(frontendRedirect)).build();
    }

    /**
     * Given a full access token response, create new authorization code and add to token cache.
     * NOTE: this endpoint is for test purposes.
     */
    @Path("/authcode")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuthCode(AccessTokenResponse accessToken) {
        String authCode = UUID.randomUUID().toString();
        tokenCache.put(authCode, accessToken);
        return Response.ok("{ \"authcode\": \"" + authCode + "\" }").build();

    }
    
    private String buildRedirect(String authCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(frontendRedirect);
        sb.append("?");
        sb.append(callbackParamName);
        sb.append("=");
        sb.append(authCode);
        return sb.toString();
    }

    private NewCookie getCookie(String authCode) {
        return new NewCookie.Builder(cookieName)
            .domain(cookieDomain)
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite(sameSite)
            .expiry(new Date(System.currentTimeMillis() + Duration.ofDays(1).toMillis()))
            .maxAge(86400)
            .value(authCode)
            .build();
    }

}
