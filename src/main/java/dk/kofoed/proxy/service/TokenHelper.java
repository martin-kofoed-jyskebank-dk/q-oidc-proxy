package dk.kofoed.proxy.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TokenHelper {

    private static final Logger logger = LoggerFactory.getLogger(TokenHelper.class);
    
    /**
     * Parse JWT string without expiry and signature validation.
     */
    public JsonWebToken parseUnverifiedToken(String jwt) throws InvalidJwtException {
        JwtConsumer consumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        JwtClaims claimsSet = consumer.processToClaims(jwt);

        return new DefaultJWTCallerPrincipal(jwt, "JWT", claimsSet);
    }

    /**
     * Check if any given token (JWT) is expired or not. 
     * Will return true if invalid token is passed.
     * Will NOT check token signature.
     * 
     * @param token - string to be parsed and checked.
     * @param gapSeconds - seconds to use as "gap" when checking expiration time. 
     */
    public boolean tokenExpired(String token, long gapSeconds) {

        if (token.startsWith("Bearer")) {
            token = token.substring(7);
        }

        try {
            JsonWebToken parsedToken = parseUnverifiedToken(token);
            Instant expirationTime = Instant.ofEpochSecond(parsedToken.getExpirationTime()).minus(gapSeconds, ChronoUnit.SECONDS);
            return Instant.now().isAfter(expirationTime);
        } catch (InvalidJwtException e) {
            logger.warn("Could not parse token - marking as expired. Message: [{}]", e.getMessage());
            return true;
        }

    }

}
