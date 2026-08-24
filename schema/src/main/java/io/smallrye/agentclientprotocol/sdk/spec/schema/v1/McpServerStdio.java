package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Stdio transport configuration for MCP.
 */
public record McpServerStdio(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("args") List<String> args,
        @JsonProperty("command") String command,
        @JsonProperty("env") List<EnvVariable> env,
        @JsonProperty("name") String name) {
    public McpServerStdio(List<String> args, String command, List<EnvVariable> env, String name) {
        this(null, args, command, env, name);
    }
}
