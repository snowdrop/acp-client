package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response to the `initialize` method.
 * 
 * Contains the negotiated protocol version and agent capabilities.
 * 
 * See protocol docs: [Initialization](https://agentclientprotocol.com/protocol/initialization)
 */
public record InitializeResponse(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("agentCapabilities") AgentCapabilities agentCapabilities,
        @JsonProperty("agentInfo") Implementation agentInfo,
        @JsonProperty("authMethods") List<Object> authMethods,
        @JsonProperty("protocolVersion") Integer protocolVersion) {
    public InitializeResponse(Integer protocolVersion) {
        this(null, null, null, null, protocolVersion);
    }
}
