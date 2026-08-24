package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response containing the terminal output and exit status.
 */
public record TerminalOutputResponse(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("exitStatus") TerminalExitStatus exitStatus,
        @JsonProperty("output") String output,
        @JsonProperty("truncated") Boolean truncated) {
    public TerminalOutputResponse(String output, Boolean truncated) {
        this(null, null, output, truncated);
    }
}
