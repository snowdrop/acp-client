package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request to write content to a text file.
 * 
 * Only available if the client supports the `fs.writeTextFile` capability.
 */
public record WriteTextFileRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("content") String content,
        @JsonProperty("path") String path,
        @JsonProperty("sessionId") String sessionId) {
    public WriteTextFileRequest(String content, String path, String sessionId) {
        this(null, content, path, sessionId);
    }
}
