package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response from creating a new session.
 * 
 * See protocol docs: [Creating a Session](https://agentclientprotocol.com/protocol/session-setup#creating-a-session)
 */
public record NewSessionResponse(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("configOptions") List<SessionConfigOption> configOptions,
        @JsonProperty("modes") SessionModeState modes,
        @JsonProperty("sessionId") String sessionId) {
    public NewSessionResponse(String sessionId) {
        this(null, null, null, sessionId);
    }
}
