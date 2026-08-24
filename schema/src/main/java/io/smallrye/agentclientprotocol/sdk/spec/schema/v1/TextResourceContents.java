package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Text-based resource contents.
 */
public record TextResourceContents(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("mimeType") String mimeType,
        @JsonProperty("text") String text,
        @JsonProperty("uri") String uri) {
    public TextResourceContents(String text, String uri) {
        this(null, null, text, uri);
    }
}
