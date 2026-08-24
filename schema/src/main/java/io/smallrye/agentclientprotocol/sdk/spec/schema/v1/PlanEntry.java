package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A single entry in the execution plan.
 * 
 * Represents a task or goal that the assistant intends to accomplish
 * as part of fulfilling the user's request.
 * See protocol docs: [Plan Entries](https://agentclientprotocol.com/protocol/agent-plan#plan-entries)
 */
public record PlanEntry(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("content") String content,
        @JsonProperty("priority") PlanEntryPriority priority,
        @JsonProperty("status") PlanEntryStatus status) {
    public PlanEntry(String content, PlanEntryPriority priority, PlanEntryStatus status) {
        this(null, content, priority, status);
    }
}
