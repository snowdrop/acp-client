package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Capabilities supported by the client.
 * 
 * Advertised during initialization to inform the agent about
 * available features and methods.
 * 
 * See protocol docs: [Client Capabilities](https://agentclientprotocol.com/protocol/initialization#client-capabilities)
 */
public record ClientCapabilities(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("fs") FileSystemCapabilities fs,
        @JsonProperty("terminal") Boolean terminal) {
}
