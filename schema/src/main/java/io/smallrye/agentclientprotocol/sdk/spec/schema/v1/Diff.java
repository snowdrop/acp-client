package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A diff representing file modifications.
 * 
 * Shows changes to files in a format suitable for display in the client UI.
 * 
 * See protocol docs: [Content](https://agentclientprotocol.com/protocol/tool-calls#content)
 */
public record Diff(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("newText") String newText,
        @JsonProperty("oldText") String oldText,
        @JsonProperty("path") String path) {
    public Diff(String newText, String path) {
        this(null, newText, null, path);
    }
}
