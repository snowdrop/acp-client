package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * The user selected one of the provided options.
 */
public record SelectedPermissionOutcome(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("outcome") String outcome,
        @JsonProperty("optionId") String optionId) {
    public SelectedPermissionOutcome(String optionId) {
        this(null, "selected", optionId);
    }
}
