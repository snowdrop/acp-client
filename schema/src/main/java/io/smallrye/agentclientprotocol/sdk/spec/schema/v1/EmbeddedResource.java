package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * The contents of a resource, embedded into a prompt or tool call result.
 */
public record EmbeddedResource(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("annotations") Annotations annotations,
        @JsonProperty("resource") Object resource) {
    public EmbeddedResource(Object resource) {
        this(null, null, resource);
    }
}
