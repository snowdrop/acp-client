package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Update to session metadata. All fields are optional to support partial updates.
 * 
 * Agents send this notification to update session information like title or custom metadata.
 * This allows clients to display dynamic session names and track session state changes.
 */
public record SessionInfoUpdate(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("title") String title,
        @JsonProperty("updatedAt") String updatedAt) {
}
