package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Session capabilities supported by the agent.
 * 
 * As a baseline, all Agents **MUST** support `session/new`, `session/prompt`, `session/cancel`, and `session/update`.
 * 
 * Optionally, they **MAY** support other session methods and notifications by specifying additional capabilities.
 * 
 * Note: `session/load` is still handled by the top-level `load_session` capability. This will be unified in future versions of the protocol.
 * 
 * See protocol docs: [Session Capabilities](https://agentclientprotocol.com/protocol/initialization#session-capabilities)
 */
public record SessionCapabilities(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("close") SessionCloseCapabilities close,
        @JsonProperty("list") SessionListCapabilities list,
        @JsonProperty("resume") SessionResumeCapabilities resume) {
}
