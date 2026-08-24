package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A resource that the server is capable of reading, included in a prompt or tool call result.
 */
public record ResourceLink(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("annotations") Annotations annotations,
        @JsonProperty("description") String description,
        @JsonProperty("mimeType") String mimeType,
        @JsonProperty("name") String name,
        @JsonProperty("size") Integer size,
        @JsonProperty("title") String title,
        @JsonProperty("uri") String uri) {
    public ResourceLink(String name, String uri) {
        this(null, null, null, null, name, null, null, uri);
    }
}
