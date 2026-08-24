package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request to release a terminal and free its resources.
 */
public record ReleaseTerminalRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("terminalId") String terminalId) {
    public ReleaseTerminalRequest(String sessionId, String terminalId) {
        this(null, sessionId, terminalId);
    }
}
