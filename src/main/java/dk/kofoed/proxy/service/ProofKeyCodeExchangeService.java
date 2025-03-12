package dk.kofoed.proxy.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kofoed.proxy.domain.AuthenticationSessionData;

@ApplicationScoped
public class ProofKeyCodeExchangeService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProofKeyCodeExchangeService.class);

    private Map<String, AuthenticationSessionData> sessionDataMap;

    /**
     * Build a <code>AuthenticationSessionData</code> instance, add it to map, and return to caller.
     */
    public AuthenticationSessionData buildNewSessionData(String state) {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        AuthenticationSessionData sessionData = new AuthenticationSessionData(state, codeChallenge, codeVerifier);
        this.sessionDataMap.put(state, sessionData);
        return sessionData;
    }

    public boolean containsState(String stateId) {
        return this.sessionDataMap.containsKey(stateId);
    }
                
    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] codeVerifierBytes = new byte[64];
        random.nextBytes(codeVerifierBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes);        
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (Exception e) {
            logger.error("Could not generate code challenge for codeVerifier = [{}]", codeVerifier);
            throw new RuntimeException("Error generating code challenge", e);
        }
    }        

    @PostConstruct
    protected void init() {
        this.sessionDataMap = new ConcurrentHashMap<>();
    }

}
