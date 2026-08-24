package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request to read content from a text file.
 * 
 * Only available if the client supports the `fs.readTextFile` capability.
 */
public record ReadTextFileRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("limit") Integer limit,
        @JsonProperty("line") Integer line,
        @JsonProperty("path") String path,
        @JsonProperty("sessionId") String sessionId) {
    public ReadTextFileRequest(String path, String sessionId) {
        this(null, null, null, path, sessionId);
    }
}
