package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents a tool call that the language model has requested.
 * 
 * Tool calls are actions that the agent executes on behalf of the language model,
 * such as reading files, executing code, or fetching data from external sources.
 * 
 * See protocol docs: [Tool Calls](https://agentclientprotocol.com/protocol/tool-calls)
 */
public record ToolCall(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("content") List<Object> content,
        @JsonProperty("kind") ToolKind kind,
        @JsonProperty("locations") List<ToolCallLocation> locations,
        @JsonProperty("rawInput") Object rawInput,
        @JsonProperty("rawOutput") Object rawOutput,
        @JsonProperty("status") ToolCallStatus status,
        @JsonProperty("title") String title,
        @JsonProperty("toolCallId") String toolCallId) {
    public ToolCall(String title, String toolCallId) {
        this(null, null, null, null, null, null, null, title, toolCallId);
    }
}
