package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Binary resource contents.
 */
public record BlobResourceContents(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("blob") String blob,
        @JsonProperty("mimeType") String mimeType,
        @JsonProperty("uri") String uri) {
    public BlobResourceContents(String blob, String uri) {
        this(null, blob, null, uri);
    }
}
