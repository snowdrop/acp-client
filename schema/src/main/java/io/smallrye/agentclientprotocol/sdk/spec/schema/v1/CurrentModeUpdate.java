package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * The current mode of the session has changed
 * 
 * See protocol docs: [Session Modes](https://agentclientprotocol.com/protocol/session-modes)
 */
public record CurrentModeUpdate(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("currentModeId") String currentModeId) {
    public CurrentModeUpdate(String currentModeId) {
        this(null, currentModeId);
    }
}
