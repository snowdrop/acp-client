package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response to terminal/release method
 */
public record ReleaseTerminalResponse(
        @JsonProperty("_meta") Map<String, Object> meta) {
}
