package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * An execution plan for accomplishing complex tasks.
 * 
 * Plans consist of multiple entries representing individual tasks or goals.
 * Agents report plans to clients to provide visibility into their execution strategy.
 * Plans can evolve during execution as the agent discovers new requirements or completes tasks.
 * 
 * See protocol docs: [Agent Plan](https://agentclientprotocol.com/protocol/agent-plan)
 */
public record Plan(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("entries") List<PlanEntry> entries) {
    public Plan(List<PlanEntry> entries) {
        this(null, entries);
    }
}
