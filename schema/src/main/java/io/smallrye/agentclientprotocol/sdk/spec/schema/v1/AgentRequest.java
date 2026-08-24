package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentRequest(
        @JsonProperty("id") Object id,
        @JsonProperty("method") String method,
        @JsonProperty("params") Object params) {
    public AgentRequest(Object id, String method) {
        this(id, method, null);
    }
}
