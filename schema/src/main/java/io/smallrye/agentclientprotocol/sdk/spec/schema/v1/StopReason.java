package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Reasons why an agent stops processing a prompt turn.
 * 
 * See protocol docs: [Stop Reasons](https://agentclientprotocol.com/protocol/prompt-turn#stop-reasons)
 */
public enum StopReason {
    END_TURN("end_turn"),
    MAX_TOKENS("max_tokens"),
    MAX_TURN_REQUESTS("max_turn_requests"),
    REFUSAL("refusal"),
    CANCELLED("cancelled");

    private final String value;

    StopReason(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
