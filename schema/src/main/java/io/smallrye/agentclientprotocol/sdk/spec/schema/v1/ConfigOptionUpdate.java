package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Session configuration options have been updated.
 */
public record ConfigOptionUpdate(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("configOptions") List<SessionConfigOption> configOptions) {
    public ConfigOptionUpdate(List<SessionConfigOption> configOptions) {
        this(null, configOptions);
    }
}
