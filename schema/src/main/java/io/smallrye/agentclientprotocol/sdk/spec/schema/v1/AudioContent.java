package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Audio provided to or from an LLM.
 */
public record AudioContent(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("annotations") Annotations annotations,
        @JsonProperty("data") String data,
        @JsonProperty("mimeType") String mimeType) {
    public AudioContent(String data, String mimeType) {
        this(null, null, data, mimeType);
    }
}
