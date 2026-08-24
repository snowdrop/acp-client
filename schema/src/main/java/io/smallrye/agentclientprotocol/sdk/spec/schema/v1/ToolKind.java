package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Categories of tools that can be invoked.
 * 
 * Tool kinds help clients choose appropriate icons and optimize how they
 * display tool execution progress.
 * 
 * See protocol docs: [Creating](https://agentclientprotocol.com/protocol/tool-calls#creating)
 */
public enum ToolKind {
    READ("read"),
    EDIT("edit"),
    DELETE("delete"),
    MOVE("move"),
    SEARCH("search"),
    EXECUTE("execute"),
    THINK("think"),
    FETCH("fetch"),
    SWITCH_MODE("switch_mode"),
    OTHER("other");

    private final String value;

    ToolKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
