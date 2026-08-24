package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * All text that was typed after the command name is provided as input.
 */
public record UnstructuredCommandInput(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("hint") String hint) {
    public UnstructuredCommandInput(String hint) {
        this(null, hint);
    }
}
