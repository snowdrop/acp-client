package io.smallrye.agentclientprotocol.sdk.spec.schema.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Prompt capabilities supported by the agent in `session/prompt` requests.
 * 
 * Baseline agent functionality requires support for [`ContentBlock::Text`]
 * and [`ContentBlock::ResourceLink`] in prompt requests.
 * 
 * Other variants must be explicitly opted in to.
 * Capabilities for different types of content in prompt requests.
 * 
 * Indicates which content types beyond the baseline (text and resource links)
 * the agent can process.
 * 
 * See protocol docs: [Prompt Capabilities](https://agentclientprotocol.com/protocol/initialization#prompt-capabilities)
 */
public record PromptCapabilities(
        @JsonProperty("_meta") Map<String, Object> meta,
        @JsonProperty("audio") Boolean audio,
        @JsonProperty("embeddedContext") Boolean embeddedContext,
        @JsonProperty("image") Boolean image) {
}
