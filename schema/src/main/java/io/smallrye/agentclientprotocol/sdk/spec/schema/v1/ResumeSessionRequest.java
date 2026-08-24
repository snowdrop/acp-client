package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request parameters for resuming an existing session.
 * 
 * Resumes an existing session without returning previous messages (unlike `session/load`).
 * This is useful for agents that can resume sessions but don't implement full session loading.
 * 
 * Only available if the Agent supports the `sessionCapabilities.resume` capability.
 */
public record ResumeSessionRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("cwd") String cwd,
        @JsonProperty("mcpServers") List<Object> mcpServers,
        @JsonProperty("sessionId") String sessionId) {
    public ResumeSessionRequest(String cwd, String sessionId) {
        this(null, cwd, null, sessionId);
    }
}
