package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Information about a session returned by session/list
 */
public record SessionInfo(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("cwd") String cwd,
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("title") String title,
        @JsonProperty("updatedAt") String updatedAt) {
    public SessionInfo(String cwd, String sessionId) {
        this(null, cwd, sessionId, null, null);
    }
}
