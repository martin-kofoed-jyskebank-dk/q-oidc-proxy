package dk.kofoed.proxy.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kofoed.proxy.client.model.AccessTokenResponse;
import dk.kofoed.proxy.exception.TokenExpiredException;
import dk.kofoed.proxy.exception.TokenNotFoundException;

@ApplicationScoped
public class TokenCache {

    private static final Logger logger = LoggerFactory.getLogger(TokenCache.class);

    private Map<String, AccessTokenResponse> cache;

    protected static final int CLEANUP_READ_INTERVAL = 1000;

    private int readCounter;

    @Inject
    TokenHelper tokenHelper;

    /**
     * Add access token response to cache.
     */
    public void put(String key, AccessTokenResponse value) {
        logger.info("Putting new token in cache. Key: [{}]", key);
        this.cache.put(key, value);
    }

    /**
     * Get access token for specified cache key. Note that token may or may not be expired at this point.
     * Periodically do cache housekeeping by calling cleanup method.
     */
    public AccessTokenResponse get(String key) throws TokenNotFoundException {
        readCounter++;
        if (readCounter == CLEANUP_READ_INTERVAL) {
            doCacheCleanup();
            readCounter = 0;
        }
        AccessTokenResponse accessToken = this.cache.get(key);
        if (accessToken == null) {
            logger.info("Token not found for key [{}]", key);
            throw new TokenNotFoundException();
        }
        logger.info("Got token from cache. Key: [{}]", key);
        return accessToken;
    }

    /**
     * Check if access_token is close to expiry. 
     */
    public Uni<AccessTokenResponse> checkRefresh(String key) throws TokenNotFoundException {
        AccessTokenResponse token = this.cache.get(key);
        if (token == null) {
            logger.info("Token not found for key [{}]", key);
            throw new TokenNotFoundException();
        }
        if (tokenHelper.tokenExpired(token.accessToken(), 10)) {
            logger.info("Token expired for key [{}]", key);
            return Uni.createFrom().failure(new TokenExpiredException("Token expired", token.refreshToken()));
        }
        return Uni.createFrom().item(token);
    }

    /**
     * Remove element from cache.
     */
    public void remove(String key) {
        this.cache.remove(key);
    }

    /**
     * Run through the cache, and remove "dead" entries defined by refresh_token expiry.
     */
    private void doCacheCleanup() {
        int counter = 0;
        Set<String> keys = this.cache.keySet();
        for (String key : keys) {
            AccessTokenResponse cachedToken = this.cache.get(key);
            if (tokenHelper.tokenExpired(cachedToken.refreshToken(), 0)) {
                counter++;
                this.cache.remove(key);
            }
        }
        logger.info("Cache cleanup done. Removed [{}] elements.", counter);
    }

    @PostConstruct
    public void init() {
        this.cache = new ConcurrentHashMap<>();
        readCounter = 0;
    }
    
}
