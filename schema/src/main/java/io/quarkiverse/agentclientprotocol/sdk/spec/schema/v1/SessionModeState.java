package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * The set of modes and the one currently active.
 */
public record SessionModeState(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("availableModes") List<SessionMode> availableModes,
        @JsonProperty("currentModeId") String currentModeId) {
    public SessionModeState(List<SessionMode> availableModes, String currentModeId) {
        this(null, availableModes, currentModeId);
    }
}
