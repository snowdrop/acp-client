# Java Library for Agent Client Protocol (ACP)

The [Agent Client Protocol](https://agentclientprotocol.com/) (ACP) is an open standard for communication between clients and AI coding agents. It defines a JSON-RPC 2.0-based protocol over stdio that lets clients initialize sessions, send prompts, receive streamed updates (thoughts, messages, tool calls, plans), and manage the agent lifecycle.

This project is a Java library for ACP, built with standard `java.util.concurrent` APIs (`CompletableFuture`, `ScheduledExecutorService`) for async operations and [Jackson](https://github.com/FasterXML/jackson) for JSON processing. It provides both synchronous and asynchronous APIs to interact with any ACP-compatible agent (e.g. [OpenCode](https://opencode.ai/)) using stdio.

The project implements the [ACP Schema Specification v1](https://agentclientprotocol.com/specification). The JSON schema definition is bundled at `schema/src/main/resources/schema/acp/v1/schema.json` and Java records are generated from it using `JSonSchemaGenerator` (a custom code generator included in the `schema` module). See [CONTRIBUTING.md](CONTRIBUTING.md) for details on regenerating schema classes.

## Project structure

The project is organized as a multi-module Maven build:

| Module   | Artifact ID     | Description                                                                                                      |
|----------|-----------------|------------------------------------------------------------------------------------------------------------------|
| `schema` | `acp-schema`    | ACP JSON Schema (`v1`), generated Java records/enums, and `JSonSchemaGenerator` code generator                   |
| `core`   | `acp-core` | ACP implementation library: `AcpClient`, `AcpAsyncClient`, `AcpSyncClient`, stdio transport. Depends on `schema` |
| `client` | `acp-client` | Aesh CLI (`AcpAgentCommand`), skills, and sandbox. Depends on `core`. Built as Quarkus uber-jar                  |

## Prerequisites

- [JDK 21+](https://openjdk.org/)
- [Apache Maven 3.9+](https://maven.apache.org/)
- Any ACP-compatible agent (see [Agents and providers](#agents-and-providers) for the list of some agents and how to install them)
- (Optional) [JBang](https://www.jbang.dev/) for running the CLI via catalog

## Build

Compile the project and build the uber-jar:
```shell
mvn clean install
```

The uber-jar is produced at `client/target/acp-client-0.1.0-SNAPSHOT-runner.jar`.

## Usage

### Running with `java -jar`

```shell
# Default prompt: "Say Hello" with OpenCode agent
java -jar client/target/acp-client-0.1.0-SNAPSHOT-runner.jar

# Custom prompt
java -jar client/target/acp-client-0.1.0-SNAPSHOT-runner.jar --prompt "What is 6+6?"

# With a specific agent, provider, and model
java -jar client/target/acp-client-0.1.0-SNAPSHOT-runner.jar \
  --agent claude \
  --provider vertex-ai \
  --model claude-opus-4-6 \
  --prompt "Say hello"
```

### Running with JBang

A [JBang catalog](https://www.jbang.dev/documentation/guide/latest/alias_catalogs.html) is provided at the project root and that you can use when you develop/test. After building:

```shell
# Run from the project root using the local catalog and uber jar generated under client/target/ !
jbang acp --prompt "What is 6+6?"
```

If you plan to use the tool outside of this project, then install it using the maven GAV
```shell
jbang app install --name acp io.quarkiverse.ai:acp-java-client:0.1.0-SNAPSHOT:runner

cd /java/project/to/code/using/ai
acp --prompt "Say hello"
```
The command supports to generate the autocompletion bash script:
```shell
source <(acp generate-completion)
```

### Running with Quarkus dev mode

```shell
mvn quarkus:dev -pl client -Dquarkus.args="--prompt 'Say Hello'"
```

### Example output

```shell
11:11:57,492 INFO  [StdioAcpClientTransport] ACP agent starting
11:11:57,522 INFO  [StdioAcpClientTransport] ACP agent started
11:11:58,435 INFO  [AcpAgentCommand] Connected to the ACP agent: OpenCode - v1.15.4
11:11:58,613 INFO  [AcpAgentCommand] Session created: ses_1b631ae8bffegMSoAYKMCI6cUc
11:11:58,619 INFO  [AcpAgentCommand] [Commands] Available:
11:11:58,622 INFO  [AcpAgentCommand] Model: opencode/big-pickle
11:11:58,623 INFO  [AcpAgentCommand] Sending prompt: Say Hello
Here is the AI response:
Hello
11:12:00,661 INFO  [AcpAgentCommand] [Usage] used=8081 size=200000 cost={amount=0, currency=USD}
11:12:00,665 INFO  [AcpAgentCommand] Done! Stop reason: END_TURN
11:12:00,676 INFO  [StdioAcpClientTransport] ACP agent process stopped (exit code 143)
```

## CLI commands

Man pages for all commands and subcommands are available in the [docs/](docs/) directory.

The following table is indicative and show for the acp client top command how you can configure the options or the corresponding environment variables. Precedence: **CLI argument > environment variable > default value**.

| Option                      | Env Variable              | Description                                                                                                                                                            | Default                      |
|-----------------------------|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|
| `-a`, `--agent`             | `ACP_AGENT`               | ACP compatible agent id (see registry list)                                                                                                                            | `opencode`                   |
| `-p`, `--prompt`            | `ACP_PROMPT`              | The prompt text to send to the agent                                                                                                                                   | `Say Hello`                  |
| `--provider`                | `ACP_PROVIDER`            | Provider: `zen`, `vertex-ai`                                                                                                                                           | `zen`                        |
| `-m`, `--model`             | `ACP_MODEL`               | The model to use, e.g. `claude-opus-4-6` (resolved per agent/provider)                                                                                                 |                              |
| `--agent-binary`            | `ACP_AGENT_BINARY`        | Override agent binary path (for custom agents)                                                                                                                         |                              |
| `--agent-args`              | `ACP_AGENT_ARGS`          | Override agent arguments (for custom agents)                                                                                                                           |                              |
| `--request-timeout`         | `ACP_REQUEST_TIMEOUT`     | Timeout in seconds for steps: initialize, create session, etc.                                                                                                         | `30`                         |
| `--prompt-timeout`          | `ACP_PROMPT_TIMEOUT`      | Timeout in seconds for prompt requests; 0 means no timeout                                                                                                             | `0`                          |
| `--permission-mode`         | `ACP_PERMISSION_MODE`     | How to respond to agent permission requests (see below)                                                                                                                | `allow_always`               |
| `-b`, `--backup`            | `ACP_BACKUP`              | Backup workspace to `target/workdirs` before running: `yes`, `no`. Only applies to Maven/Gradle projects. When enabled, the session CWD is set to the backup directory | `yes`                        |
| `--backup-project-name`     | `ACP_BACKUP_PROJECT_NAME` | Name of the project used in the backup directory: `target/workdirs/<name>_<timestamp>`                                                                                 | `.` (current directory name) |
| `--wks`, `--workspace-path` | `WORKSPACE_PATH`          | Absolute path to the project/workspace directory used as CWD for the session. If not set, defaults to the directory where the command is executed                      | current directory            |
| `-l`, `--log-level`         | `ACP_LOG_LEVEL`           | Log level: `INFO`, `DEBUG`, `TRACE`, `WARNING`, `SEVERE`                                                                                                               | `INFO`                       |
| `-h`, `--help`              |                           | Show help message and exit                                                                                                                                             |                              |
| `-V`, `--version`           |                           | Print version info and exit                                                                                                                                            |                              |

The `--agent` option resolves the binary and arguments automatically from a built-in registry. For custom or unsupported agents, use `--agent-binary` and `--agent-args` instead.

When using `--agent opencode` with `--provider vertex-ai`, simple model names are resolved automatically:
`--model claude-opus-4-6` becomes `google-vertex-anthropic/claude-opus-4-6@default`.

### Examples

For more detailed command examples per agent and provider, see [COMMANDS_EXAMPLE.md](COMMANDS_EXAMPLE.md).

```shell
# OpenCode with Zen (default agent + provider)
acp --prompt "Say Hello"

# Claude Code with Vertex AI
acp --agent claude --provider vertex-ai --model claude-opus-4-6 \
  --prompt "Say Hello"

# Using environment variables
export ACP_AGENT=claude
export ACP_PROVIDER=vertex-ai
export ACP_MODEL=claude-opus-4-6
acp --prompt "Execute the java-project-discovery skill."

# Gemini CLI
acp --agent gemini --prompt "Say Hello"

# Custom agent binary
acp --agent-binary my-agent --agent-args "serve" --prompt "Say Hello"
```

## Agents and providers

### ACP agents

The following ACP-compatible agents can be used with this client. Install the agent you need and pass its binary and args via the `--agent-binary` and `--agent-args` CLI options.

| Agent       | Binary (`--agent-binary`) | Args (`--agent-args`) | Installation                                                                                                                       |
|-------------|---------------------------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------|
| OpenCode    | `opencode`                | `acp`                 | See [OpenCode ACP docs](https://opencode.ai/docs/acp/)                                                                            |
| Claude Code | `claude-agent-acp`        |                       | `npm install -g @agentclientprotocol/claude-agent-acp` ([docs](https://www.npmjs.com/package/@agentclientprotocol/claude-agent-acp)) |
| Pi          | `pi-acp`                  |                       | `npm install -g pi-acp` ([docs](https://github.com/svkozak/pi-acp))                                                               |
| Gemini CLI  | `gemini`                  | `--acp`               | `npm install -g @google/gemini-cli` ([docs](https://geminicli.com/docs/cli/acp-mode/))                                             |

### Providers

Each agent can be configured with a model provider. The `--provider` option controls which environment variables are validated before connecting. The client will exit with an error if any required variable is missing.

| Agent       | Provider (`--provider`) | Environment variables                                                                         | Documentation                                                                         |
|-------------|-------------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| OpenCode    | `zen` (default) | none                                                                                          | [OpenCode Zen](https://opencode.ai/docs/zen/)                                        |
| OpenCode    | `vertex-ai` | `GOOGLE_APPLICATION_CREDENTIALS`, `VERTEX_LOCATION`, `GOOGLE_CLOUD_PROJECT`                   | [Google Vertex AI](https://opencode.ai/docs/providers/#google-vertex-ai)              |
| Claude Code | `vertex-ai` | `ANTHROPIC_VERTEX_PROJECT_ID`, `ANTHROPIC_MODEL`, `CLAUDE_CODE_USE_VERTEX`, `CLOUD_ML_REGION` | [Anthropic Vertex AI](https://docs.anthropic.com/en/docs/build-with-claude/vertex-ai) |
| Pi          | `vertex-ai` | `GOOGLE_APPLICATION_CREDENTIALS`, `GOOGLE_CLOUD_PROJECT`, `CLOUD_ML_REGION`                   | [pi-vertex-claude](https://github.com/isaacraja/pi-vertex-claude)                    |
| Gemini CLI  | `vertex-ai` | `GOOGLE_CLOUD_PROJECT`, `GOOGLE_CLOUD_LOCATION`                                               | [Gemini CLI ACP mode](https://geminicli.com/docs/cli/acp-mode/)                      |

## Permissions

When an agent needs to perform a sensitive operation (e.g. writing a file, running a command), it sends a `session/request_permission` request. The client responds automatically based on the `--permission-mode` value:

| Mode           | Behavior                                            |
|----------------|-----------------------------------------------------|
| `allow_always` | Accept and remember the choice (default)            |
| `allow_once`   | Accept only this time                               |
| `reject_once`  | Reject only this time                               |
| `reject_always`| Reject and remember the choice                      |

Example:
```shell
java -jar client/target/acp-client-0.1.0-SNAPSHOT-runner.jar \
  --permission-mode allow_once \
  --prompt "Create a Java HelloWorld class"
```

## Workspace path and backup

### Workspace path

The `--workspace-path` option sets the project directory used as CWD for the agent session. If not specified, it defaults to the directory where the command is executed.

```shell
# Run the agent against a different project directory
acp --agent claude --workspace-path /path/to/my-project --prompt "Say hello"

# Using an environment variable
export WORKSPACE_PATH=/path/to/my-project
acp --agent claude --prompt "Say hello"
```

### Workspace backup

When running against a Maven or Gradle project, the client automatically backs up the workspace before starting the agent session. This creates a timestamped copy of your source files under `target/workdirs/`, allowing you to restore the original state if the agent makes undesired changes. When backup is enabled and succeeds, the session CWD is automatically set to the backup directory so the agent works on the copy.

- **Enabled by default** (`--backup yes`)
- **Only applies** to directories containing `pom.xml`, `build.gradle`, or `build.gradle.kts`
- **Backup path**: `target/workdirs/<workspace-name>_<yyyyMMdd-HHmmss>`
- **CWD moves to backup**: when backup succeeds, the agent session CWD points to the backup directory
- **`--backup-project-name`** defaults to `.`, which resolves to the current directory name. Override it when running the same command against multiple projects in a shared workspace
- **Excludes** build output and metadata directories: `target/`, `build/`, `.git/`, `.gradle/`, `.idea/`, `node_modules/`
- **Skipped silently** for non-Maven/Gradle workspaces regardless of the flag value

```shell
# Backup is enabled by default — uses current directory name
# CWD is set to the backup directory
acp --agent claude --prompt "Refactor the service layer"
# → CWD: target/workdirs/my-project_20260526-143022/

# Specify a backup project name (useful when running against multiple projects)
acp --agent claude --backup-project-name my-service --prompt "Migrate to Jakarta"
# → CWD: target/workdirs/my-service_20260526-143022/

# Combine workspace-path with backup
acp --agent claude --workspace-path /path/to/my-project --prompt "Refactor"
# → CWD: /path/to/my-project/target/workdirs/my-project_20260526-143022/

# Disable backup — CWD stays as workspace-path or current directory
acp --agent claude --backup no --prompt "Refactor the service layer"
```

## Logging

The project uses Quarkus logging (backed by [JBoss Log Manager](https://github.com/jboss-logging/jboss-logmanager)). By default, only `INFO`-level messages are shown (connection status, prompt lifecycle). Session update details (thoughts, tool calls, plans, commands, usage) and protocol internals are logged at `DEBUG` or `TRACE` level.

Log levels are configured in `client/src/main/resources/application.properties`. You can also override them on the command line:

```shell
# Enable debug logging
java -Dquarkus.log.category.\"io.quarkiverse\".level=DEBUG \
  -jar client/target/acp-client-0.1.0-SNAPSHOT-runner.jar \
  --prompt "Say Hello"

# Enable trace logging (raw JSON-RPC messages)
java -Dquarkus.log.category.\"io.quarkiverse\".level=TRACE \
  -jar client/target/acp-client-0.1.0-SNAPSHOT-runner.jar \
  --prompt "Say Hello"
```

### Log levels

| Level | What you see |
|-------|-------------|
| `INFO` (default) | Connected to agent, sending prompt, stop reason |
| `DEBUG` | + agent thoughts, tool calls, plans, commands, mode changes, usage, capabilities, session ID |
| `TRACE` | + raw JSON-RPC messages sent/received by the transport |
