package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The type of permission option being presented to the user.
 * 
 * Helps clients choose appropriate icons and UI treatment.
 */
public enum PermissionOptionKind {
    ALLOW_ONCE("allow_once"),
    ALLOW_ALWAYS("allow_always"),
    REJECT_ONCE("reject_once"),
    REJECT_ALWAYS("reject_always");

    private final String value;

    PermissionOptionKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
