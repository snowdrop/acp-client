package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A session configuration option selector and its current state.
 */
public record SessionConfigOption(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("category") Object category,
        @JsonProperty("currentValue") String currentValue,
        @JsonProperty("description") String description,
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("type") String type) {
    public SessionConfigOption(String id, String name) {
        this(null, null, null, null, id, name, null);
    }
}
