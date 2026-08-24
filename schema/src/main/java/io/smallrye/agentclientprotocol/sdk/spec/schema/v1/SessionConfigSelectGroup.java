package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * A group of possible values for a session configuration option.
 */
public record SessionConfigSelectGroup(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("group") String group,
        @JsonProperty("name") String name,
        @JsonProperty("options") List<SessionConfigSelectOption> options) {
    public SessionConfigSelectGroup(String group, String name, List<SessionConfigSelectOption> options) {
        this(null, group, name, options);
    }
}
