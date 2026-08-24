package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response containing the contents of a text file.
 */
public record ReadTextFileResponse(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("content") String content) {
    public ReadTextFileResponse(String content) {
        this(null, content);
    }
}
