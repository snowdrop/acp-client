package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC error object.
 * 
 * Represents an error that occurred during method execution, following the
 * JSON-RPC 2.0 error object specification with optional additional data.
 * 
 * See protocol docs: [JSON-RPC Error Object](https://www.jsonrpc.org/specification#error_object)
 */
public record Error(
        @JsonProperty("code") Object code,
        @JsonProperty("data") Object data,
        @JsonProperty("message") String message) {
    public Error(Object code, String message) {
        this(code, null, message);
    }
}
