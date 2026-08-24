package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * An image provided to or from an LLM.
 */
public record ImageContent(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("annotations") Annotations annotations,
        @JsonProperty("data") String data,
        @JsonProperty("mimeType") String mimeType,
        @JsonProperty("uri") String uri) {
    public ImageContent(String data, String mimeType) {
        this(null, null, data, mimeType, null);
    }
}
