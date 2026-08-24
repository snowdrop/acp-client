package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * An environment variable to set when launching an MCP server.
 */
public record EnvVariable(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("name") String name,
        @JsonProperty("value") String value) {
    public EnvVariable(String name, String value) {
        this(null, name, value);
    }
}
