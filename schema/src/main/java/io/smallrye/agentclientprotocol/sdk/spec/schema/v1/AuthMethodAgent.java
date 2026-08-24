package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Agent handles authentication itself.
 * 
 * This is the default authentication method type.
 */
public record AuthMethodAgent(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("description") String description,
        @JsonProperty("id") String id,
        @JsonProperty("name") String name) {
    public AuthMethodAgent(String id, String name) {
        this(null, null, id, name);
    }
}
