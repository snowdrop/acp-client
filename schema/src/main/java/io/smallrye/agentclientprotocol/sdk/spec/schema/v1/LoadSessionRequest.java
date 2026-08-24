package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request parameters for loading an existing session.
 * 
 * Only available if the Agent supports the `loadSession` capability.
 * 
 * See protocol docs: [Loading Sessions](https://agentclientprotocol.com/protocol/session-setup#loading-sessions)
 */
public record LoadSessionRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("cwd") String cwd,
        @JsonProperty("mcpServers") List<Object> mcpServers,
        @JsonProperty("sessionId") String sessionId) {
    public LoadSessionRequest(String cwd, List<Object> mcpServers, String sessionId) {
        this(null, cwd, mcpServers, sessionId);
    }
}
