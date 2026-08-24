package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A mode the agent can operate in.
 * 
 * See protocol docs: [Session Modes](https://agentclientprotocol.com/protocol/session-modes)
 */
public record SessionMode(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("description") String description,
        @JsonProperty("id") String id,
        @JsonProperty("name") String name) {
    public SessionMode(String id, String name) {
        this(null, null, id, name);
    }
}
