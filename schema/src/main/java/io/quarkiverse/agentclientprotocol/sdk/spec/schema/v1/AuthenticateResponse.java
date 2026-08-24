package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response to the `authenticate` method.
 */
public record AuthenticateResponse(
        @JsonProperty("_meta") Map<String, Object> meta) {
}
