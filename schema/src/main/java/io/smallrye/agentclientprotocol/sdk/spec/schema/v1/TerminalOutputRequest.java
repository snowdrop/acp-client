package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request to get the current output and status of a terminal.
 */
public record TerminalOutputRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("terminalId") String terminalId) {
    public TerminalOutputRequest(String sessionId, String terminalId) {
        this(null, sessionId, terminalId);
    }
}
