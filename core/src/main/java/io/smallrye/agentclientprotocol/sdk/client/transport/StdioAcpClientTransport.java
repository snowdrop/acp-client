package io.smallrye.agentclientprotocol.sdk.client.transport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.smallrye.agentclientprotocol.sdk.spec.schema.v1.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * ACP stdio transport.
 *
 * <p>Communicates with an ACP agent subprocess using standard input/output streams.
 * Messages are exchanged as newline-delimited JSON-RPC 2.0 over stdin/stdout.
 *
 * <p>Threading model:
 * <ul>
 *   <li><b>inbound</b> — daemon thread reading from the agent's stdout, dispatching
 *       parsed JSON messages to the registered {@link #setInboundMessageHandler handler}</li>
 *   <li><b>outbound</b> — daemon thread consuming a {@link LinkedBlockingQueue}
 *       and writing serialized JSON to the agent's stdin</li>
 *   <li><b>error</b> — daemon thread forwarding the agent's stderr to a configurable handler</li>
 * </ul>
 *
 * <p>The default {@link ObjectMapper} is configured with:
 * <ul>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false} for forward compatibility</li>
 *   <li>{@code NON_NULL} serialization to avoid sending null fields</li>
 *   <li>A custom {@link TextContent} serializer that adds the required {@code "type": "text"} discriminator</li>
 * </ul>
 *
 * @see AgentParameters
 */
public class StdioAcpClientTransport {

    private static final Logger logger = LoggerFactory.getLogger(StdioAcpClientTransport.class);

    private final AgentParameters params;
    private final ObjectMapper mapper;

    private Process process;
    private volatile boolean isClosing = false;

    // Outbound: client → agent (JSON-RPC requests)
    private final LinkedBlockingQueue<JsonNode> outboundQueue = new LinkedBlockingQueue<>();

    // Thread pools — daemon threads so JVM can exit if closeGracefully() isn't called
    private final ExecutorService inboundExecutor;
    private final ExecutorService outboundExecutor;
    private final ExecutorService errorExecutor;

    private Consumer<String> stdErrorHandler = error -> logger.info("STDERR: {}", error);
    private Consumer<JsonNode> inboundMessageHandler;

    /**
     * Creates a transport with the default {@link ObjectMapper} configuration.
     *
     * @param params the agent process configuration
     */
    public StdioAcpClientTransport(AgentParameters params) {
        this(params, createDefaultMapper());
    }

    private static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // ACP server requires "type" discriminator on content objects
        SimpleModule module = new SimpleModule("AcpContentTypes");
        module.addSerializer(TextContent.class, new TextContentSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Custom serializer that adds the required "type": "text" field
     * when serializing TextContent for the ACP protocol.
     */
    static class TextContentSerializer extends StdSerializer<TextContent> {
        TextContentSerializer() { super(TextContent.class); }

        @Override
        public void serialize(TextContent value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("type", "text");
            gen.writeStringField("text", value.text());
            if (value.annotations() != null) {
                gen.writeObjectField("annotations", value.annotations());
            }
            gen.writeEndObject();
        }
    }

    /**
     * Creates a transport with a custom {@link ObjectMapper}.
     *
     * @param params the agent process configuration
     * @param mapper the Jackson mapper for JSON serialization/deserialization
     */
    public StdioAcpClientTransport(AgentParameters params, ObjectMapper mapper) {
        this.params = params;
        this.mapper = mapper;

        this.inboundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "acp-client-inbound");
            t.setDaemon(true);
            return t;
        });
        this.outboundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "acp-client-outbound");
            t.setDaemon(true);
            return t;
        });
        this.errorExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "acp-client-error");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Sets the handler for incoming JSON-RPC messages (responses and notifications).
     *
     * @param handler the message consumer
     */
    public void setInboundMessageHandler(Consumer<JsonNode> handler) {
        this.inboundMessageHandler = handler;
    }

    /**
     * Sets the handler for the agent's stderr output. Defaults to logging at INFO level.
     *
     * @param handler the stderr line consumer
     */
    public void setStdErrorHandler(Consumer<String> handler) {
        this.stdErrorHandler = handler;
    }

    /**
     * Launches the agent process and starts the inbound, outbound, and error processing threads.
     */
    public void connect() {
        logger.info("ACP agent starting");

        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(params.getCommand());
        fullCommand.addAll(params.getArgs());

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(fullCommand);
        processBuilder.environment().putAll(params.getEnv());

        try {
            this.process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start process: " + fullCommand, e);
        }

        if (process.getInputStream() == null || process.getOutputStream() == null) {
            process.destroy();
            throw new RuntimeException("Process input or output stream is null");
        }

        startInboundProcessing();
        startOutboundProcessing();
        startErrorProcessing();

        logger.info("ACP agent started");
    }

    /**
     * Sends a JSON-RPC message to the agent process via the outbound queue.
     *
     * @param message the JSON message to send
     */
    public void sendMessage(JsonNode message) {
        outboundQueue.add(message);
    }

    private void startInboundProcessing() {
        inboundExecutor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while (!isClosing && (line = reader.readLine()) != null) {
                    try {
                        logger.trace("RECV: {}", line);
                        JsonNode message = mapper.readTree(line);
                        if (inboundMessageHandler != null) {
                            inboundMessageHandler.accept(message);
                        }
                    } catch (Exception e) {
                        if (!isClosing) {
                            logger.error("Error processing inbound message: {}", line, e);
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                if (!isClosing) {
                    logger.error("Error reading from input stream", e);
                }
            } finally {
                isClosing = true;
            }
        });
    }

    private void startOutboundProcessing() {
        outboundExecutor.execute(() -> {
            try {
                while (!isClosing) {
                    JsonNode message = outboundQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null && !isClosing) {
                        try {
                            String jsonMessage = mapper.writeValueAsString(message);
                            jsonMessage = jsonMessage.replace("\r\n", "\\n")
                                    .replace("\n", "\\n")
                                    .replace("\r", "\\n");
                            logger.trace("SEND: {}", jsonMessage);

                            var os = process.getOutputStream();
                            synchronized (os) {
                                os.write(jsonMessage.getBytes(StandardCharsets.UTF_8));
                                os.write("\n".getBytes(StandardCharsets.UTF_8));
                                os.flush();
                            }
                        } catch (IOException e) {
                            if (!isClosing) {
                                logger.error("Error writing outbound message", e);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void startErrorProcessing() {
        errorExecutor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while (!isClosing && (line = reader.readLine()) != null) {
                    stdErrorHandler.accept(line);
                }
            } catch (IOException e) {
                if (!isClosing) {
                    logger.error("Error reading from error stream", e);
                }
            }
        });
    }

    /**
     * Gracefully shuts down the transport: drains the outbound queue,
     * sends SIGTERM to the agent process, waits up to 5 seconds for exit,
     * and shuts down all executor threads.
     */
    public void closeGracefully() {
        isClosing = true;
        logger.debug("Initiating graceful shutdown");

        if (process != null) {
            logger.debug("Sending TERM to process");
            process.destroy();
            try {
                boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                if (exited) {
                    int exitCode = process.exitValue();
                    if (exitCode == 0 || exitCode == 143 || exitCode == 137) {
                        logger.info("ACP agent process stopped (exit code {})", exitCode);
                    } else {
                        logger.warn("Process terminated with code {}", exitCode);
                    }
                } else {
                    logger.warn("Process did not exit within timeout, forcing kill");
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Interrupted while waiting for process exit");
            }
        }

        inboundExecutor.shutdownNow();
        outboundExecutor.shutdownNow();
        errorExecutor.shutdownNow();
        try {
            inboundExecutor.awaitTermination(2, TimeUnit.SECONDS);
            outboundExecutor.awaitTermination(2, TimeUnit.SECONDS);
            errorExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.debug("Graceful shutdown completed");
    }

    /**
     * Returns the {@link ObjectMapper} used by this transport for JSON processing.
     *
     * @return the configured mapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }
}
