package dk.kofoed.proxy.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record OpenIdConfigurationResponse(

    String issuer,
    @JsonProperty("authorization_endpoint")
    String authorizationEndpoint,
    @JsonProperty("token_endpoint")
    String tokenEndpoint,
    @JsonProperty("token_introspection_endpoint")
    String tokenIntrospectionEndpoint,
    @JsonProperty("userinfo_endpoint")
    String userinfoEndpoint,
    @JsonProperty("jwks_uri")
    String jwksUri

) {} 