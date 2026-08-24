package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A possible value for a session configuration option.
 */
public record SessionConfigSelectOption(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("description") String description,
        @JsonProperty("name") String name,
        @JsonProperty("value") String value) {
    public SessionConfigSelectOption(String name, String value) {
        this(null, null, name, value);
    }
}
