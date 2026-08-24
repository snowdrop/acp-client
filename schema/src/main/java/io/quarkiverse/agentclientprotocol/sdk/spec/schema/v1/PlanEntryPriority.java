package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Priority levels for plan entries.
 * 
 * Used to indicate the relative importance or urgency of different
 * tasks in the execution plan.
 * See protocol docs: [Plan Entries](https://agentclientprotocol.com/protocol/agent-plan#plan-entries)
 */
public enum PlanEntryPriority {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    private final String value;

    PlanEntryPriority(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
