package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * An update to an existing tool call.
 * 
 * Used to report progress and results as tools execute. All fields except
 * the tool call ID are optional - only changed fields need to be included.
 * 
 * See protocol docs: [Updating](https://agentclientprotocol.com/protocol/tool-calls#updating)
 */
public record ToolCallUpdate(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("content") List<Object> content,
        @JsonProperty("kind") ToolKind kind,
        @JsonProperty("locations") List<ToolCallLocation> locations,
        @JsonProperty("rawInput") Object rawInput,
        @JsonProperty("rawOutput") Object rawOutput,
        @JsonProperty("status") ToolCallStatus status,
        @JsonProperty("title") String title,
        @JsonProperty("toolCallId") String toolCallId) {
    public ToolCallUpdate(String toolCallId) {
        this(null, null, null, null, null, null, null, null, toolCallId);
    }
}
