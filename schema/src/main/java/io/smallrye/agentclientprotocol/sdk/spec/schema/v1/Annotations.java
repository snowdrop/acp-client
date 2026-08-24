package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Optional annotations for the client. The client can use annotations to inform how objects are used or displayed
 */
public record Annotations(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("audience") List<Role> audience,
        @JsonProperty("lastModified") String lastModified,
        @JsonProperty("priority") Double priority) {
}
