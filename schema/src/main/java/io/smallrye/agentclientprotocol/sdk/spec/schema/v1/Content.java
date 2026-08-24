package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Standard content block (text, images, resources).
 */
public record Content(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("content") Object content) {
    public Content(Object content) {
        this(null, content);
    }
}
