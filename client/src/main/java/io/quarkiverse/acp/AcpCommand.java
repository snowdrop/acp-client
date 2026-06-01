package io.quarkiverse.acp;

import io.quarkiverse.acp.registry.AcpRegistryManager;
import io.quarkiverse.acp.registry.RegistryCommand;
import io.quarkiverse.agentclientprotocol.sdk.client.AcpClient;
import io.quarkiverse.agentclientprotocol.sdk.client.AcpSyncClient;
import io.quarkiverse.agentclientprotocol.sdk.client.transport.AgentParameters;
import io.quarkiverse.agentclientprotocol.sdk.client.transport.StdioAcpClientTransport;
import io.quarkiverse.agentclientprotocol.sdk.spec.schema.v1.*;
import org.jboss.logging.Logger;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Aesh CLI command for any ACP-compatible agent (OpenCode, Claude, Pi, Gemini, etc.).
 *
 * <p>Connects to an ACP agent over stdio, initializes a session,
 * sends a prompt, and streams session updates (thoughts, messages, tool calls, plans)
 * to the console.
 *
 * <p>Each option can also be set via an environment variable (shown in brackets).
 * Precedence: CLI argument &gt; environment variable &gt; default value.
 *
 * <p>Usage:
 * <pre>{@code
 * # Using a known agent (resolves binary and args automatically)
 * acp --agent claude-acp --provider vertex-ai --model claude-opus-4-6 --prompt "Say hello"
 *
 * # Using a custom agent binary
 * acp --agent-binary my-agent --agent-args "serve" --prompt "Say hello"
 * }</pre>
 */
@GroupCommandDefinition(
        name = "acp",
        description = "acp tool for any acp compatible agent (OpenCode, Claude, Pi, Gemini, etc.)",
        generateHelp = true,
        groupCommands = {RegistryCommand.class}
)
public class AcpCommand implements Command<CommandInvocation> {

    private static final Logger logger = Logger.getLogger(AcpCommand.class);

    // -- Agent resolution ----
    // Agents are resolved dynamically from the ACP registry.
    // Use 'acp install <agent-id>' to install an agent first.
    // Agent IDs match the ACP registry (e.g. opencode, claude-acp, pi-acp, gemini).

    private final AcpRegistryManager registryManager = new AcpRegistryManager();

    // -- Provider env-var requirements per agent + provider ----
    // Key format: "agent-id:provider". Checked before launching the agent.

    private static final Map<String, List<String>> PROVIDER_ENV_VARS = Map.ofEntries(
            Map.entry("opencode:zen",       List.of()),
            Map.entry("opencode:vertex-ai", List.of("GOOGLE_APPLICATION_CREDENTIALS", "VERTEX_LOCATION", "GOOGLE_CLOUD_PROJECT")),
            Map.entry("claude-acp:vertex-ai",   List.of("ANTHROPIC_VERTEX_PROJECT_ID", "CLAUDE_CODE_USE_VERTEX", "CLOUD_ML_REGION")),
            Map.entry("pi-acp:vertex-ai",       List.of("GOOGLE_APPLICATION_CREDENTIALS", "GOOGLE_CLOUD_PROJECT", "CLOUD_ML_REGION")),
            Map.entry("gemini:vertex-ai",   List.of("GOOGLE_CLOUD_PROJECT"))
    );

    // -- Instance state ----

    private final StringBuilder thoughtBuffer = new StringBuilder();
    private volatile boolean messageOutputPending = false;

    // -- CLI options ----

    @Option(shortName = 'a', name = "agent",
            description = "ACP agent registry ID: opencode, claude-acp, pi-acp, gemini, ... (use 'acp reg list --registry' to see all) [env: ACP_AGENT]")
    String agent;

    @Option(name = "agent-binary",
            description = "Override agent binary path (for custom agents) [env: ACP_AGENT_BINARY]")
    String acpAgentBinary;

    @Option(name = "agent-args",
            description = "Override agent arguments (for custom agents) [env: ACP_AGENT_ARGS]")
    String acpAgentArgs;

    @Option(shortName = 'p', name = "prompt",
            description = "The prompt text to send to the agent [env: ACP_PROMPT]")
    String prompt;

    @Option(shortName = 'm', name = "model",
            description = "The model to use, e.g. claude-opus-4-6 (resolved per agent/provider) [env: ACP_MODEL]")
    String model;

    @Option(name = "provider",
            description = "Provider: zen, vertex-ai [env: ACP_PROVIDER]")
    String provider;

    @Option(name = "request-timeout",
            description = "Timeout in seconds for requests (initialize, create session, etc.) [env: ACP_REQUEST_TIMEOUT]")
    Integer requestTimeout;

    @Option(name = "prompt-timeout",
            description = "Timeout in seconds for prompt requests; 0 means no timeout [env: ACP_PROMPT_TIMEOUT]")
    Integer promptTimeout;

    @Option(name = "permission-mode",
            description = "How to respond to agent permission requests: allow_always, allow_once, reject_once, reject_always [env: ACP_PERMISSION_MODE]")
    String permissionMode;

    @Option(shortName = 'b', name = "backup",
            description = "Backup workspace to target/workdirs before running: yes, no (default: yes). Only applies to Maven/Gradle projects [env: ACP_BACKUP]")
    String backup;

    @Option(name = "backup-project-name",
            description = "Name of the project used in the backup directory: target/workdirs/<name>_<timestamp> (default: current directory name) [env: ACP_BACKUP_PROJECT_NAME]")
    String backupProjectName;

    @Option(aliases = "wks",
            name = "workspace-path",
            description = "Absolute path to the project/workspace directory used as CWD for the session. If not set, defaults to the directory where the command is executed [env: WORKSPACE_PATH]")
    String workspacePath;

    @Option(shortName = 's', name = "skill-path",
            description = "Absolute path to a skills folder to add as additional directory [env: SKILL_PATH]")
    String skillPath;

    @Option(shortName = 'l', name = "log-level",
            description = "Log level: INFO, DEBUG, TRACE, WARNING, SEVERE [env: ACP_LOG_LEVEL]")
    String logLevel;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        // Configure log level if provided
        logLevel = resolveOption(logLevel, "ACP_LOG_LEVEL", null);
        if (logLevel != null && !logLevel.isEmpty()) {
            Level level = Level.parse(logLevel.toUpperCase());
            java.util.logging.Logger.getLogger("io.quarkiverse").setLevel(level);
            java.util.logging.Logger.getLogger("io.quarkiverse.acp").setLevel(level);
            java.util.logging.Logger.getLogger("io.quarkiverse.agentclientprotocol").setLevel(level);
            java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
            if (rootLogger.getLevel().intValue() > level.intValue()) {
                rootLogger.setLevel(level);
            }
            for (var handler : rootLogger.getHandlers()) {
                if (handler.getLevel().intValue() > level.intValue()) {
                    handler.setLevel(level);
                }
            }
        }

        // Resolve options: CLI arg > env var > default
        prompt = resolveOption(prompt, "ACP_PROMPT", "Say Hello");
        permissionMode = resolveOption(permissionMode, "ACP_PERMISSION_MODE", "allow_always");

        // -- Resolve agent binary and args ----
        agent = resolveOption(agent, "ACP_AGENT", "opencode");
        acpAgentBinary = resolveOption(acpAgentBinary, "ACP_AGENT_BINARY", null);
        acpAgentArgs = resolveOption(acpAgentArgs, "ACP_AGENT_ARGS", null);

        String binary;
        String args;
        if (acpAgentBinary != null) {
            binary = acpAgentBinary;
            args = acpAgentArgs;
        } else {
            var agentCommand = registryManager.resolveAgentCommand(agent);
            if (agentCommand != null) {
                binary = agentCommand.binary();
                args = acpAgentArgs != null ? acpAgentArgs
                        : String.join(",", agentCommand.args());
            } else {
                AcpRegistryManager.Registry registry = registryManager.getCachedRegistry();
                if (registry != null && registryManager.findAgent(registry, agent) != null) {
                    invocation.println("ERROR: Agent '" + agent
                            + "' exists in the ACP registry but is not installed.");
                    invocation.println("Run:  acp reg install " + agent);
                } else {
                    invocation.println("ERROR: Unknown agent '" + agent + "'.");
                    invocation.println("Run:  acp reg list --registry   to see available agents.");
                    invocation.println("      acp reg install <id>      to install one.");
                }
                invocation.println("Alternatively, use --agent-binary to specify the agent binary directly.");
                return CommandResult.FAILURE;
            }
        }

        // -- Resolve and normalize provider ----
        provider = resolveOption(provider, "ACP_PROVIDER", "zen");
        provider = normalizeProvider(provider);

        // -- Resolve model name ----
        model = resolveOption(model, "ACP_MODEL", null);
        if (model != null) {
            model = resolveModelName(agent, provider, model);
        }

        // -- Timeouts ----
        String reqTimeoutStr = resolveOption(
                requestTimeout != null ? requestTimeout.toString() : null,
                "ACP_REQUEST_TIMEOUT", "30");
        Duration reqTimeout = Duration.ofSeconds(Long.parseLong(reqTimeoutStr));

        String promptTimeoutStr = resolveOption(
                promptTimeout != null ? promptTimeout.toString() : null,
                "ACP_PROMPT_TIMEOUT", "0");
        long promptTimeoutSecs = Long.parseLong(promptTimeoutStr);
        Duration pTimeout = promptTimeoutSecs > 0 ? Duration.ofSeconds(promptTimeoutSecs) : null;

        // 0. Check for required env variables based on agent + provider
        checkProviderEnv(agent, provider);

        // 0b. Resolve workspace path: CLI/env > current directory
        workspacePath = resolveOption(workspacePath, "WORKSPACE_PATH", null);
        String sessionCwd = workspacePath != null ? workspacePath : System.getProperty("user.dir");
        logger.infof("Workspace CWD: %s", sessionCwd);

        // 0c. Backup workspace if requested and project is Maven/Gradle
        backup = resolveOption(backup, "ACP_BACKUP", "yes");
        backupProjectName = resolveOption(backupProjectName, "ACP_BACKUP_PROJECT_NAME", ".");
        if ("yes".equalsIgnoreCase(backup)) {
            Path backupDir = backupWorkspace(backupProjectName, Path.of(sessionCwd));
            if (backupDir != null) {
                sessionCwd = backupDir.toAbsolutePath().toString();
                logger.infof("CWD set to backup directory: %s", sessionCwd);
            }
        }
        final String cwd = sessionCwd;

        // 1. Configure agent parameters
        var paramBuilder = AgentParameters.builder(binary);
        if (args != null && !args.isEmpty()) {
            for (String a : args.split(",")) {
                String trimmed = a.trim();
                if (!trimmed.isEmpty()) {
                    paramBuilder.arg(trimmed);
                }
            }
        }
        var params = paramBuilder.build();

        // 2. Create transport
        var transport = new StdioAcpClientTransport(params);

        // 3. Build sync client with session update consumer and permission handler
        final String permMode = permissionMode;
        try (AcpSyncClient client = AcpClient.sync(transport)
                .requestTimeout(reqTimeout)
                .promptTimeout(pTimeout)
                .sessionUpdateConsumer(notification -> {
                    String updateType = notification.meta() != null
                            ? (String) notification.meta().get("sessionUpdate") : null;
                    handleSessionUpdate(updateType, notification.update());
                })
                .permissionRequestHandler(request -> handlePermissionRequest(request, permMode))
                .build()) {

            // 4. Initialize -- handshake with the agent
            var initResponse = client.initialize();
            var agentInfo = initResponse.agentInfo();
            String title = agentInfo.title();
            String connectedMsg = (title != null && !title.isEmpty())
                    ? String.format("Connected to the ACP agent: %s - v%s - %s", agentInfo.name(), agentInfo.version(), title)
                    : String.format("Connected to the ACP agent: %s - v%s", agentInfo.name(), agentInfo.version());
            logger.info(connectedMsg);
            logger.infof("Protocol version: %s", initResponse.protocolVersion());
            logger.debugf("Capabilities: %s", initResponse.agentCapabilities());
            logger.debugf("Auth methods: %s", initResponse.authMethods());

            // 5. Create a session
            var session = client.newSession(new NewSessionRequest(cwd, List.of()));
            var sessionId = session.sessionId();
            logger.infof("Session created: %s with CWD: %s", sessionId, cwd);

            // Log the agent's default model from session config
            if (session.configOptions() != null) {
                session.configOptions().stream()
                        .filter(opt -> "model".equalsIgnoreCase(opt.id()))
                        .findFirst()
                        .ifPresent(opt -> logger.infof("Agent model: %s", opt.currentValue()));
            }

            // 6. Set the model (only if explicitly provided)
            if (model != null && !model.isEmpty()) {
                try {
                    var configResponse = client.setConfigOption(
                            new SetSessionConfigOptionRequest("model", sessionId, model));
                    if (configResponse.configOptions() != null) {
                        configResponse.configOptions().stream()
                                .filter(opt -> "model".equalsIgnoreCase(opt.id()))
                                .findFirst()
                                .ifPresent(opt -> logger.infof("Model set to: %s", opt.currentValue()));
                    }
                } catch (RuntimeException e) {
                    if (e.getMessage() != null && e.getMessage().contains("-32601")) {
                        logger.warnf("Agent does not support session/set_config_option -- skipping model configuration. "
                                + "The agent will use its default model.");
                    } else {
                        throw e;
                    }
                }
            }

            // 7. Send a prompt
            skillPath = resolveOption(skillPath, "SKILL_PATH", null);
            String effectivePrompt = prompt;
            if (skillPath != null) {
                effectivePrompt = prompt + "\n\nPlease read the skill file at: " + skillPath + " and follow its instructions.";
            }
            logger.infof("Sending prompt: %s", effectivePrompt);
            invocation.println("Here is the AI response:");
            var response = client.prompt(new PromptRequest(
                    List.of(new TextContent(effectivePrompt)),
                    sessionId
            ));

            flushThoughts();
            if (messageOutputPending) {
                // TODO: Migration required -- System.out.println() used here because session update
                // callbacks (handleSessionUpdate) write directly to System.out and this newline
                // must go to the same stream for correct interleaving.
                System.out.println();
                messageOutputPending = false;
            }
            logger.infof("Done! Stop reason: %s", response.stopReason());

            return CommandResult.SUCCESS;
        } catch (Exception e) {
            invocation.println("Error: " + e.getMessage());
            e.printStackTrace();
            return CommandResult.FAILURE;
        }
    }

    // -- Provider normalization ----

    private static String normalizeProvider(String provider) {
        return switch (provider) {
            case "opencode-zen","zen"      -> "zen";
            case "vertex-ai","google-vertex-ai",
                 "anthropic-vertex-ai" -> "vertex-ai";
            default                  -> provider;
        };
    }

    // -- Model name resolution ----

    private static String resolveModelName(String agent, String provider, String model) {
        if (model.contains("/")) {
            return model;
        }
        if ("opencode".equals(agent) && "vertex-ai".equals(provider)) {
            return "google-vertex-anthropic/" + model + "@default";
        }
        return model;
    }

    // -- Option resolution ----

    private static String resolveOption(String cliValue, String envVar, String defaultValue) {
        if (cliValue != null && !cliValue.isEmpty()) {
            return cliValue;
        }
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return defaultValue;
    }

    // -- Session update handling ----
    // NOTE: These handlers use System.out.print/println because they are invoked as callbacks
    // from the ACP client transport layer, where CommandInvocation is not available.
    // Refactoring to use invocation.println() would require changes to the ACP client API.

    private void handleSessionUpdate(String updateType, Object update) {
        if (update == null) {
            logger.debug("[Update] null");
            return;
        }

        if (!"agent_thought_chunk".equals(updateType)) {
            flushThoughts();
        }

        if (!"agent_message_chunk".equals(updateType) && messageOutputPending) {
            System.out.println();
            messageOutputPending = false;
        }

        switch (update) {
            case ContentChunk chunk -> {
                if ("agent_thought_chunk".equals(updateType)) {
                    thoughtBuffer.append(extractText(chunk.content()));
                } else {
                    System.out.print(extractText(chunk.content()));
                    messageOutputPending = true;
                }
            }
            case Plan plan -> {
                logger.infof("[Plan] %d steps:", plan.entries().size());
                plan.entries().forEach(e -> logger.infof("  - %s [%s]", e.content(), e.status()));
            }
            case ToolCall tool ->
                logger.infof("[ToolCall] %s (%s) - %s", tool.title(), tool.kind(), tool.status());
            case ToolCallUpdate toolUpdate ->
                logger.infof("[ToolUpdate] %s - %s", toolUpdate.title(), toolUpdate.status());
            case AvailableCommandsUpdate commands -> {
                logger.info("[Commands] Available:");
                commands.availableCommands()
                        .forEach(c -> logger.debugf("  /%s - %s", c.name(), c.description()));
            }
            case ConfigOptionUpdate configUpdate -> {
                if (configUpdate.configOptions() != null) {
                    configUpdate.configOptions().stream()
                            .filter(opt -> "model".equalsIgnoreCase(opt.id()))
                            .findFirst()
                            .ifPresent(opt -> logger.infof("Model changed: %s", opt.currentValue()));
                }
                logger.infof("[Config] %s", configUpdate.configOptions());
            }
            case CurrentModeUpdate mode -> logger.infof("[Mode] %s", mode.currentModeId());
            default -> {
                if ("usage_update".equals(updateType) && update instanceof Map<?,?> map) {
                    logger.infof("[Usage] used=%s size=%s cost=%s", map.get("used"), map.get("size"), map.get("cost"));
                } else {
                    logger.infof("[Update] %s: %s", updateType, update);
                }
            }
        }
    }

    private void flushThoughts() {
        if (!thoughtBuffer.isEmpty()) {
            logger.debugf("[Thought] %s", thoughtBuffer.toString().strip());
            thoughtBuffer.setLength(0);
        }
    }

    private static String extractText(Object content) {
        if (content instanceof Map<?,?> map) {
            Object text = map.get("text");
            return text != null ? text.toString() : content.toString();
        }
        return content != null ? content.toString() : "";
    }

    // -- Permission handling ----

    private static RequestPermissionResponse handlePermissionRequest(RequestPermissionRequest request, String permissionMode) {
        var toolCall = request.toolCall();
        logger.infof("[Permission] %s requests: %s", toolCall.title(), toolCall.kind());

        String selectedOptionId = request.options().stream()
                .filter(o -> o.kind().getValue().equals(permissionMode))
                .findFirst()
                .map(PermissionOption::optionId)
                .orElseGet(() -> request.options().stream()
                        .filter(o -> o.kind() == PermissionOptionKind.ALLOW_ALWAYS
                                || o.kind() == PermissionOptionKind.ALLOW_ONCE)
                        .findFirst()
                        .map(PermissionOption::optionId)
                        .orElse(request.options().getFirst().optionId()));

        logger.infof("[Permission] Responded with: %s", permissionMode);
        return new RequestPermissionResponse(new SelectedPermissionOutcome(selectedOptionId));
    }

    // -- Provider env-var validation ----

    private static void checkProviderEnv(String agent, String provider) {
        String key = agent + ":" + provider;
        List<String> requiredVars = PROVIDER_ENV_VARS.get(key);

        if (requiredVars == null) {
            if (!"zen".equals(provider)) {
                logger.warnf("No env var requirements defined for agent '%s' with provider '%s'", agent, provider);
            }
            return;
        }

        for (String varName : requiredVars) {
            requireEnv(varName, provider);
        }
    }

    private static void requireEnv(String varName, String provider) {
        String value = System.getenv(varName);
        if (value == null || value.isBlank()) {
            System.err.println("ERROR: " + varName + " environment variable is not set (required for " + provider + " provider).");
            System.exit(1);
        }
    }

    // -- Workspace backup ----

    private Path backupWorkspace(String name, Path workDir) {
        boolean isMaven = Files.exists(workDir.resolve("pom.xml"));
        boolean isGradle = Files.exists(workDir.resolve("build.gradle"))
                || Files.exists(workDir.resolve("build.gradle.kts"));

        if (!isMaven && !isGradle) {
            logger.info("Skipping workspace backup (not a Maven or Gradle project)");
            return null;
        }

        String projectName = ".".equals(name) ? workDir.getFileName().toString() : name;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
        Path backupDir = workDir.resolve("target").resolve("workdirs").resolve(projectName + "_" + timestamp);

        Set<String> excludes = Set.of("target", "build", ".git", ".gradle", ".idea", "node_modules",".claude", ".env");

        try {
            Files.walk(workDir)
                    .filter(path -> {
                        Path relative = workDir.relativize(path);
                        return relative.getNameCount() == 0
                                || !excludes.contains(relative.getName(0).toString());
                    })
                    .forEach(source -> {
                        Path dest = backupDir.resolve(workDir.relativize(source));
                        try {
                            if (Files.isDirectory(source)) {
                                Files.createDirectories(dest);
                            } else {
                                Files.createDirectories(dest.getParent());
                                Files.copy(source, dest);
                            }
                        } catch (IOException e) {
                            logger.warnf("Failed to copy %s: %s", source, e.getMessage());
                        }
                    });
            logger.infof("Workspace backed up to: %s", backupDir);
            return backupDir;
        } catch (IOException e) {
            logger.warnf("Failed to backup workspace: %s", e.getMessage());
            return null;
        }
    }
}
