package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request parameters for sending a user prompt to the agent.
 * 
 * Contains the user's message and any additional context.
 * 
 * See protocol docs: [User Message](https://agentclientprotocol.com/protocol/prompt-turn#1-user-message)
 */
public record PromptRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("prompt") List<Object> prompt,
        @JsonProperty("sessionId") String sessionId) {
    public PromptRequest(List<Object> prompt, String sessionId) {
        this(null, prompt, sessionId);
    }
}
