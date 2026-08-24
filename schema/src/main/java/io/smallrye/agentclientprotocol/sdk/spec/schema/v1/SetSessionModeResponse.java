package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response to `session/set_mode` method.
 */
public record SetSessionModeResponse(
        @JsonProperty("_meta") Map<String, Object> meta) {
}
