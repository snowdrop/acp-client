package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request to create a new terminal and execute a command.
 */
public record CreateTerminalRequest(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("args") List<String> args,
        @JsonProperty("command") String command,
        @JsonProperty("cwd") String cwd,
        @JsonProperty("env") List<EnvVariable> env,
        @JsonProperty("outputByteLimit") Integer outputByteLimit,
        @JsonProperty("sessionId") String sessionId) {
    public CreateTerminalRequest(String command, String sessionId) {
        this(null, null, command, null, null, null, sessionId);
    }
}
