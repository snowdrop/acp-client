package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request parameters for setting a session configuration option.
 */
public record SetSessionConfigOptionRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("configId") String configId,
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("value") String value) {
    public SetSessionConfigOptionRequest(String configId, String sessionId, String value) {
        this(null, configId, sessionId, value);
    }
}
