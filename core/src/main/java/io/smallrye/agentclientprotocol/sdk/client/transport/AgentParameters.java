package io.smallrye.agentclientprotocol.sdk.client.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration for launching an ACP agent process via stdio.
 *
 * <p>Encapsulates the command, arguments, and environment variables needed to
 * start the agent subprocess. A safe subset of the host environment is inherited
 * by default (platform-dependent).
 *
 * <p>Use the {@link Builder} to construct instances:
 * <pre>{@code
 * var params = AgentParameters.builder("opencode")
 *         .arg("acp")
 *         .addEnvVar("OPENCODE_MODEL", "anthropic/claude-sonnet")
 *         .build();
 * }</pre>
 */
public class AgentParameters {

    private static final List<String> DEFAULT_INHERITED_ENV_VARS = System.getProperty("os.name")
        .toLowerCase()
        .contains("win")
                ? List.of("APPDATA", "HOMEDRIVE", "HOMEPATH", "LOCALAPPDATA", "PATH",
                        "PROCESSOR_ARCHITECTURE", "SYSTEMDRIVE", "SYSTEMROOT", "TEMP", "USERNAME", "USERPROFILE")
                : List.of("HOME", "LOGNAME", "PATH", "SHELL", "TERM", "USER");

    private final String command;
    private final List<String> args;
    private final Map<String, String> env;

    private AgentParameters(String command, List<String> args, Map<String, String> env) {
        this.command = command;
        this.args = args;
        this.env = new HashMap<>(getDefaultEnvironment());
        if (env != null && !env.isEmpty()) {
            this.env.putAll(env);
        }
    }

    /** @return the executable command (e.g. {@code "opencode"}) */
    public String getCommand() { return command; }

    /** @return the command-line arguments */
    public List<String> getArgs() { return args; }

    /** @return the environment variables for the subprocess */
    public Map<String, String> getEnv() { return env; }

    /**
     * Creates a new builder for the given command.
     *
     * @param command the executable to launch (e.g. {@code "opencode"})
     * @return a new {@link Builder}
     */
    public static Builder builder(String command) {
        return new Builder(command);
    }

    /** Fluent builder for {@link AgentParameters}. */
    public static class Builder {
        private final String command;
        private final List<String> args = new ArrayList<>();
        private final Map<String, String> env = new HashMap<>();

        public Builder(String command) {
            this.command = command;
        }

        /**
         * Adds multiple command-line arguments.
         *
         * @param args the arguments to append
         * @return this builder
         */
        public Builder args(String... args) {
            this.args.addAll(Arrays.asList(args));
            return this;
        }

        /**
         * Adds a single command-line argument.
         *
         * @param arg the argument to append
         * @return this builder
         */
        public Builder arg(String arg) {
            this.args.add(arg);
            return this;
        }

        /**
         * Adds an environment variable for the subprocess.
         *
         * @param key   the variable name
         * @param value the variable value
         * @return this builder
         */
        public Builder addEnvVar(String key, String value) {
            this.env.put(key, value);
            return this;
        }

        /**
         * Builds the {@link AgentParameters} instance.
         *
         * @return the configured parameters
         */
        public AgentParameters build() {
            return new AgentParameters(command, args, env);
        }
    }

    private static Map<String, String> getDefaultEnvironment() {
        return System.getenv().entrySet().stream()
                .filter(e -> DEFAULT_INHERITED_ENV_VARS.contains(e.getKey()))
                .filter(e -> e.getValue() != null)
                .filter(e -> !e.getValue().startsWith("()"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
