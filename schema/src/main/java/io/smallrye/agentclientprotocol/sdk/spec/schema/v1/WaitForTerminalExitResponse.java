package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response containing the exit status of a terminal command.
 */
public record WaitForTerminalExitResponse(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("exitCode") Integer exitCode,
        @JsonProperty("signal") String signal) {
}
