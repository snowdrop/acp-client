package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * HTTP transport configuration for MCP.
 */
public record McpServerHttp(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("headers") List<HttpHeader> headers,
        @JsonProperty("name") String name,
        @JsonProperty("url") String url) {
    public McpServerHttp(List<HttpHeader> headers, String name, String url) {
        this(null, headers, name, url);
    }
}
