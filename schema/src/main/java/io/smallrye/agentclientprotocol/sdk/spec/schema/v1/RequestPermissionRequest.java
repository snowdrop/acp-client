package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request for user permission to execute a tool call.
 * 
 * Sent when the agent needs authorization before performing a sensitive operation.
 * 
 * See protocol docs: [Requesting Permission](https://agentclientprotocol.com/protocol/tool-calls#requesting-permission)
 */
public record RequestPermissionRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("options") List<PermissionOption> options,
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("toolCall") ToolCallUpdate toolCall) {
    public RequestPermissionRequest(List<PermissionOption> options, String sessionId, ToolCallUpdate toolCall) {
        this(null, options, sessionId, toolCall);
    }
}
