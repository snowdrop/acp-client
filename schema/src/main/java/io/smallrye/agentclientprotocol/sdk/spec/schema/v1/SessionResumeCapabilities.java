package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Capabilities for the `session/resume` method.
 * 
 * By supplying `{}` it means that the agent supports resuming of sessions.
 */
public record SessionResumeCapabilities(
        @JsonProperty("_meta") Map<String, Object> meta) {
}
