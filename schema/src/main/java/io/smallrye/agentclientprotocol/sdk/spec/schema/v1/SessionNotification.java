package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Notification containing a session update from the agent.
 * 
 * Used to stream real-time progress and results during prompt processing.
 * 
 * See protocol docs: [Agent Reports Output](https://agentclientprotocol.com/protocol/prompt-turn#3-agent-reports-output)
 */
public record SessionNotification(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("update") Object update) {
    public SessionNotification(String sessionId, Object update) {
        this(null, sessionId, update);
    }
}
