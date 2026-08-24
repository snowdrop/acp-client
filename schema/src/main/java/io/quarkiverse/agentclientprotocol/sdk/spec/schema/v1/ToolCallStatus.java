package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Execution status of a tool call.
 * 
 * Tool calls progress through different statuses during their lifecycle.
 * 
 * See protocol docs: [Status](https://agentclientprotocol.com/protocol/tool-calls#status)
 */
public enum ToolCallStatus {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    ToolCallStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
