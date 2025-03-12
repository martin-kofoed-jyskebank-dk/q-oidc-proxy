package dk.kofoed.proxy.domain;

public record AuthenticationSessionData(
    String state,
    String codeChallenge,
    String codeVerifier
) {}
