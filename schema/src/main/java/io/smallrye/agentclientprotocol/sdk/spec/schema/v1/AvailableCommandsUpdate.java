package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Available commands are ready or have changed
 */
public record AvailableCommandsUpdate(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("availableCommands") List<AvailableCommand> availableCommands) {
    public AvailableCommandsUpdate(List<AvailableCommand> availableCommands) {
        this(null, availableCommands);
    }
}
