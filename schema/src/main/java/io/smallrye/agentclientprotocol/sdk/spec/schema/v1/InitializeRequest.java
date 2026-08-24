package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request parameters for the initialize method.
 * 
 * Sent by the client to establish connection and negotiate capabilities.
 * 
 * See protocol docs: [Initialization](https://agentclientprotocol.com/protocol/initialization)
 */
public record InitializeRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("clientCapabilities") ClientCapabilities clientCapabilities,
        @JsonProperty("clientInfo") Implementation clientInfo,
        @JsonProperty("protocolVersion") Integer protocolVersion) {
    public InitializeRequest(Integer protocolVersion) {
        this(null, null, null, protocolVersion);
    }
}
