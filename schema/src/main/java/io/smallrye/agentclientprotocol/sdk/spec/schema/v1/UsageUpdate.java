package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Usage update streamed during prompt processing.
 * Contains resource usage metrics such as token counts and cost information.
 */
public record UsageUpdate(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("used") Integer used,
        @JsonProperty("size") Integer size,
        @JsonProperty("cost") Map<String, Object> cost) {
    public UsageUpdate(Integer used, Integer size, Map<String, Object> cost) {
        this(null, used, size, cost);
    }
}
