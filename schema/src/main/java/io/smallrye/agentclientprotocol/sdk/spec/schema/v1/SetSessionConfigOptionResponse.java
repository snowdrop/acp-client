package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response to `session/set_config_option` method.
 */
public record SetSessionConfigOptionResponse(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("configOptions") List<SessionConfigOption> configOptions) {
    public SetSessionConfigOptionResponse(List<SessionConfigOption> configOptions) {
        this(null, configOptions);
    }
}
