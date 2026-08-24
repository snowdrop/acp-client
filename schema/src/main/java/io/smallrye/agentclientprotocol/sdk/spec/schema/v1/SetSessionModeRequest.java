package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request parameters for setting a session mode.
 */
public record SetSessionModeRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("modeId") String modeId,
        @JsonProperty("sessionId") String sessionId) {
    public SetSessionModeRequest(String modeId, String sessionId) {
        this(null, modeId, sessionId);
    }
}
