package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A file location being accessed or modified by a tool.
 * 
 * Enables clients to implement "follow-along" features that track
 * which files the agent is working with in real-time.
 * 
 * See protocol docs: [Following the Agent](https://agentclientprotocol.com/protocol/tool-calls#following-the-agent)
 */
public record ToolCallLocation(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("line") Integer line,
        @JsonProperty("path") String path) {
    public ToolCallLocation(String path) {
        this(null, null, path);
    }
}
