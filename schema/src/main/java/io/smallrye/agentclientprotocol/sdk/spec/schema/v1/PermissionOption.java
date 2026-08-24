package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * An option presented to the user when requesting permission.
 */
public record PermissionOption(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("kind") PermissionOptionKind kind,
        @JsonProperty("name") String name,
        @JsonProperty("optionId") String optionId) {
    public PermissionOption(PermissionOptionKind kind, String name, String optionId) {
        this(null, kind, name, optionId);
    }
}
