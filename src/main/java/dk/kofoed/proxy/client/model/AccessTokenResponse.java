package dk.kofoed.proxy.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record AccessTokenResponse(
    @JsonProperty("access_token")
    String accessToken,
    @JsonProperty("expires_in")
    int expireSeconds,
    @JsonProperty("refresh_expires_in")
    int refreshExpireSeconds,
    @JsonProperty("refresh_token")
    String refreshToken,
    @JsonProperty("id_token")
    String idToken,
    @JsonProperty("token_type")
    String tokenType,
    @JsonProperty("session_state")
    String sessionState,
    String scope
) {}
