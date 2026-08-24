package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Capabilities supported by the agent.
 * 
 * Advertised during initialization to inform the client about
 * available features and content types.
 * 
 * See protocol docs: [Agent Capabilities](https://agentclientprotocol.com/protocol/initialization#agent-capabilities)
 */
public record AgentCapabilities(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("loadSession") Boolean loadSession,
        @JsonProperty("mcpCapabilities") McpCapabilities mcpCapabilities,
        @JsonProperty("promptCapabilities") PromptCapabilities promptCapabilities,
        @JsonProperty("sessionCapabilities") SessionCapabilities sessionCapabilities) {
}
