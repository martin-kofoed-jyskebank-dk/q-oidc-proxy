package dk.kofoed.proxy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public record RedirectDataResponse(
    @JsonProperty("redirect_to")
    String redirectTo
) {}
