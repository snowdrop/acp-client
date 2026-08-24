package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Metadata about the implementation of the client or agent.
 * Describes the name and version of an MCP implementation, with an optional
 * title for UI representation.
 */
public record Implementation(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("name") String name,
        @JsonProperty("title") String title,
        @JsonProperty("version") String version) {
    public Implementation(String name, String version) {
        this(null, name, null, version);
    }
}
