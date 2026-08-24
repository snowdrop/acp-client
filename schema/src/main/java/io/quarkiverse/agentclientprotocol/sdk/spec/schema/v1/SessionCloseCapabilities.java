package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Capabilities for the `session/close` method.
 * 
 * By supplying `{}` it means that the agent supports closing of sessions.
 */
public record SessionCloseCapabilities(
        @JsonProperty("_meta") Map<String, Object> meta) {
}
