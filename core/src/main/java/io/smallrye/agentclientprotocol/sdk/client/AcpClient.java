package io.smallrye.agentclientprotocol.sdk.client;

import io.smallrye.agentclientprotocol.sdk.client.transport.StdioAcpClientTransport;
import io.smallrye.agentclientprotocol.sdk.spec.schema.v1.RequestPermissionRequest;
import io.smallrye.agentclientprotocol.sdk.spec.schema.v1.RequestPermissionResponse;
import io.smallrye.agentclientprotocol.sdk.spec.schema.v1.SessionNotification;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Factory for creating ACP clients.
 *
 * <p>Use {@link #sync(StdioAcpClientTransport)} for blocking operations
 * or {@link #async(StdioAcpClientTransport)} for non-blocking Uni-based operations.
 */
public final class AcpClient {

    private AcpClient() {}

    /**
     * Creates a builder for a synchronous (blocking) ACP client.
     *
     * @param transport the stdio transport to use
     * @return a {@link SyncBuilder} for configuring and building the client
     */
    public static SyncBuilder sync(StdioAcpClientTransport transport) {
        return new SyncBuilder(transport);
    }

    /**
     * Creates a builder for an asynchronous ({@link java.util.concurrent.CompletableFuture}-based) ACP client.
     *
     * @param transport the stdio transport to use
     * @return an {@link AsyncBuilder} for configuring and building the client
     */
    public static AsyncBuilder async(StdioAcpClientTransport transport) {
        return new AsyncBuilder(transport);
    }

    /** Builder for configuring and creating an {@link AcpSyncClient}. */
    public static class SyncBuilder {
        private final StdioAcpClientTransport transport;
        private Duration requestTimeout = Duration.ofSeconds(30);
        private Duration promptTimeout;
        private Consumer<SessionNotification> sessionUpdateConsumer;
        private Function<RequestPermissionRequest, RequestPermissionResponse> permissionRequestHandler;

        private SyncBuilder(StdioAcpClientTransport transport) {
            this.transport = transport;
        }

        /**
         * Sets the timeout for individual JSON-RPC requests. Defaults to 30 seconds.
         *
         * @param timeout the request timeout duration
         * @return this builder
         */
        public SyncBuilder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        /**
         * Sets the timeout for prompt requests. Defaults to no timeout since prompts
         * can run for extended periods while the agent processes tool calls.
         *
         * @param timeout the prompt timeout duration, or {@code null} for no timeout
         * @return this builder
         */
        public SyncBuilder promptTimeout(Duration timeout) {
            this.promptTimeout = timeout;
            return this;
        }

        /**
         * Sets the consumer for session update notifications streamed during prompt processing.
         *
         * @param consumer the notification consumer
         * @return this builder
         */
        public SyncBuilder sessionUpdateConsumer(Consumer<SessionNotification> consumer) {
            this.sessionUpdateConsumer = consumer;
            return this;
        }

        /**
         * Sets the handler for permission requests from the agent.
         * If not set, permissions are auto-accepted with the first allow option.
         *
         * @param handler function that receives the permission request and returns a response
         * @return this builder
         */
        public SyncBuilder permissionRequestHandler(Function<RequestPermissionRequest, RequestPermissionResponse> handler) {
            this.permissionRequestHandler = handler;
            return this;
        }

        /**
         * Builds and connects the synchronous client.
         *
         * @return a connected {@link AcpSyncClient}
         */
        public AcpSyncClient build() {
            AcpAsyncClient async = new AcpAsyncClient(transport, requestTimeout, promptTimeout, sessionUpdateConsumer, permissionRequestHandler);
            return new AcpSyncClient(async);
        }
    }

    /** Builder for configuring and creating an {@link AcpAsyncClient}. */
    public static class AsyncBuilder {
        private final StdioAcpClientTransport transport;
        private Duration requestTimeout = Duration.ofSeconds(30);
        private Duration promptTimeout;
        private Consumer<SessionNotification> sessionUpdateConsumer;
        private Function<RequestPermissionRequest, RequestPermissionResponse> permissionRequestHandler;

        private AsyncBuilder(StdioAcpClientTransport transport) {
            this.transport = transport;
        }

        /** @see SyncBuilder#requestTimeout(Duration) */
        public AsyncBuilder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        /** @see SyncBuilder#promptTimeout(Duration) */
        public AsyncBuilder promptTimeout(Duration timeout) {
            this.promptTimeout = timeout;
            return this;
        }

        /** @see SyncBuilder#sessionUpdateConsumer(Consumer) */
        public AsyncBuilder sessionUpdateConsumer(Consumer<SessionNotification> consumer) {
            this.sessionUpdateConsumer = consumer;
            return this;
        }

        /** @see SyncBuilder#permissionRequestHandler(Function) */
        public AsyncBuilder permissionRequestHandler(Function<RequestPermissionRequest, RequestPermissionResponse> handler) {
            this.permissionRequestHandler = handler;
            return this;
        }

        /**
         * Builds the asynchronous client. Call {@link AcpAsyncClient#connect()} to start it.
         *
         * @return an {@link AcpAsyncClient} (not yet connected)
         */
        public AcpAsyncClient build() {
            return new AcpAsyncClient(transport, requestTimeout, promptTimeout, sessionUpdateConsumer, permissionRequestHandler);
        }
    }
}
