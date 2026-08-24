package io.smallrye.agentclientprotocol.sdk.client;

import io.smallrye.agentclientprotocol.sdk.spec.schema.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Synchronous ACP client that wraps {@link AcpAsyncClient} with blocking calls.
 *
 * <p>Implements {@link AutoCloseable} for use with try-with-resources.
 * The constructor eagerly connects to the agent process and blocks until ready.
 *
 * <p>Example usage:
 * <pre>{@code
 * try (AcpSyncClient client = AcpClient.sync(transport)
 *         .sessionUpdateConsumer(n -> handleUpdate(n.update()))
 *         .build()) {
 *     var init = client.initialize();
 *     var session = client.newSession(new NewSessionRequest(".", List.of()));
 *     var response = client.prompt(new PromptRequest(
 *             List.of(new TextContent("Hello")), session.sessionId()));
 * }
 * }</pre>
 *
 * @see AcpClient
 * @see AcpAsyncClient
 */
public class AcpSyncClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AcpSyncClient.class);
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(10);

    private final AcpAsyncClient delegate;

    /**
     * Creates a sync client wrapping the given async client.
     * Eagerly connects to the agent process, blocking up to 30 seconds.
     *
     * @param delegate the async client to wrap
     */
    AcpSyncClient(AcpAsyncClient delegate) {
        this.delegate = delegate;
        awaitWithTimeout(this.delegate.connect(), Duration.ofSeconds(30));
    }

    @Override
    public void close() {
        closeGracefully();
    }

    /**
     * Gracefully shuts down the client and the agent process.
     *
     * @return {@code true} if shutdown completed within the timeout, {@code false} otherwise
     */
    public boolean closeGracefully() {
        try {
            logger.debug("Gracefully closing ACP sync client");
            awaitWithTimeout(delegate.closeGracefully(), DEFAULT_CLOSE_TIMEOUT);
            return true;
        } catch (RuntimeException e) {
            logger.warn("Client didn't close within timeout", e);
            return false;
        }
    }

    /**
     * Performs the ACP initialization handshake (blocking).
     *
     * @param request the initialization parameters
     * @return the agent's capabilities and metadata
     */
    public InitializeResponse initialize(InitializeRequest request) {
        return await(delegate.initialize(request));
    }

    /**
     * Performs the ACP initialization handshake with default capabilities (blocking).
     *
     * @return the agent's capabilities and metadata
     */
    public InitializeResponse initialize() {
        return await(delegate.initialize());
    }

    /**
     * Creates a new agent session (blocking).
     *
     * @param request the session parameters
     * @return the session ID and available modes
     */
    public NewSessionResponse newSession(NewSessionRequest request) {
        return await(delegate.newSession(request));
    }

    /**
     * Sends a user prompt and blocks until the agent completes its response.
     *
     * @param request the prompt content and session ID
     * @return the stop reason when the agent finishes
     */
    public PromptResponse prompt(PromptRequest request) {
        return await(delegate.prompt(request));
    }

    /**
     * Sets a session configuration option, e.g. the model (blocking).
     *
     * @param request the config option ID, value, and session ID
     * @return the updated config options
     */
    public SetSessionConfigOptionResponse setConfigOption(SetSessionConfigOptionRequest request) {
        return await(delegate.setConfigOption(request));
    }

    /**
     * Sends a cancellation notification for the current prompt turn (blocking).
     *
     * @param notification the session ID to cancel
     */
    public void cancel(CancelNotification notification) {
        await(delegate.cancel(notification));
    }

    // ===== Helpers =====

    /**
     * Blocks indefinitely for a future to complete, unwrapping checked exceptions.
     */
    private <T> T await(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw unwrap(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * Blocks for a future to complete within the given timeout, unwrapping checked exceptions.
     */
    private <T> T awaitWithTimeout(CompletableFuture<T> future, Duration timeout) {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw unwrap(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out after " + timeout, e);
        }
    }

    /**
     * Unwraps an {@link ExecutionException}, preserving {@link RuntimeException} types.
     */
    private static RuntimeException unwrap(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException re) {
            return re;
        }
        return new RuntimeException(cause);
    }
}
