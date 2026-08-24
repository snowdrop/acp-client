package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request parameters for closing an active session.
 * 
 * If supported, the agent **must** cancel any ongoing work related to the session
 * (treat it as if `session/cancel` was called) and then free up any resources
 * associated with the session.
 * 
 * Only available if the Agent supports the `sessionCapabilities.close` capability.
 */
public record CloseSessionRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("sessionId") String sessionId) {
    public CloseSessionRequest(String sessionId) {
        this(null, sessionId);
    }
}
