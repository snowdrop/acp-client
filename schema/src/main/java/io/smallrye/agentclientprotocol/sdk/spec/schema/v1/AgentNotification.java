package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentNotification(
        @JsonProperty("method") String method,
        @JsonProperty("params") Object params) {
    public AgentNotification(String method) {
        this(method, null);
    }
}
