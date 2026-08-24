package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response containing the ID of the created terminal.
 */
public record CreateTerminalResponse(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("terminalId") String terminalId) {
    public CreateTerminalResponse(String terminalId) {
        this(null, terminalId);
    }
}
