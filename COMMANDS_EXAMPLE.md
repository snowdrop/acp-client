# ACP Client - JBang Commands

Reference commands for running the ACP Java Client with each supported agent and provider.

## Table of contents

- [Prerequisites](#prerequisites)
- [OpenCode](#opencode)
  - [OpenCode + Zen](#opencode--zen-free-model-no-env-vars-needed)
  - [OpenCode + Vertex AI](#opencode--vertex-ai)
- [Claude Code](#claude-code)
  - [Claude Code + Vertex AI](#claude-code--vertex-ai)
- [Gemini CLI](#gemini-cli)
- [Pi](#pi)
  - [Pi + Vertex AI](#pi--vertex-ai)
- [Using --skill-path](#using---skill-path)
- [Using --workspace-path](#using---workspace-path)
- [Using --backup and --backup-project-name](#using---backup-and---backup-project-name)
- [Using --log-level](#using---log-level)

## Prerequisites

- **Build the uber-jar** first with `mvn clean install`
- Load the environment variables with `dotenv -x .env` or `export KEY=VAR` or `set -x KEY VAR` using the appropriate mechanism of your shell.
- Have an ACP compatible client installed: pi, opencode, claude, etc. See [Agents and providers](README.md#agents-and-providers)
- Install the JBang Java `acp-client`: `jbang app install --name acp io.quarkiverse.ai:acp-java-client:0.1.0-SNAPSHOT:runner`

## OpenCode

### OpenCode + Zen (free model, no env vars needed)

```shell
acp  \
  --prompt "Say Hello"
```

```shell
acp \
  --prompt "Read the skills/dummy/SKILL.md instructions and say hello at the root of the project."
```

### OpenCode + Vertex AI

```shell
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json
export VERTEX_LOCATION=<google-location>
export GOOGLE_CLOUD_PROJECT=<your-gcp-project>
```

You can specify the model 
```shell
acp \
  --provider vertex-ai \
  --model claude-opus-4-6 \
  --prompt "Say Hello"
```

or use the default `claude-opus-4-6`
```shell
acp \
  --provider vertex-ai \
  --prompt "Execute the **java-project-discovery** skill. Inspect the workspace root directory, determine the build setup, target Java version, and framework configurations, and return the structured JSON output."
```

## Claude Code

### Claude Code + Vertex AI

```shell
export ANTHROPIC_VERTEX_PROJECT_ID=<your-gcp-project>
export CLAUDE_CODE_USE_VERTEX=1
export CLOUD_ML_REGION=<google-location>
```

```shell
acp \
  --agent claude \
  --provider vertex-ai \
  --model claude-opus-4-6 \
  --prompt "Say Hello"
```
or use the default model: `claude-opus-4-6` using the provider `vertex-ai`
```shell
acp \
  --agent claude \
  --provider vertex-ai \
  --prompt "Read the skills/dummy/SKILL.md instructions and say hello at the root of the project."
```

```shell
acp \
  --agent claude \
  --provider vertex-ai \
  --prompt "Execute the **java-project-discovery** skill. Inspect the workspace root directory, determine the build setup, target Java version, and framework configurations, and return the structured JSON output."
```

## Gemini CLI

### Gemini CLI (uses Google Cloud SDK authentication)

```shell
acp \
  --agent gemini \
  --prompt "Say Hello"
```

```shell
acp \
  --agent gemini \
  --prompt "Read the skills/dummy/SKILL.md instructions and say hello at the root of the project."
```

## Pi

### Pi + Vertex AI

```shell
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json
export GOOGLE_CLOUD_PROJECT=<your-gcp-project>
export CLOUD_ML_REGION=<google-location>
```

```shell
acp \
  --agent pi \
  --provider vertex-ai \
  --prompt "Say Hello"
```

```shell
acp \
  --agent pi \
  --provider vertex-ai \
  --prompt "Read the skills/dummy/SKILL.md instructions and say hello at the root of the project."
```

## Using --skill-path

The `--skill-path` option (or `SKILL_PATH` env var) specifies the path to a skill file. When set, the prompt is enhanced with an instruction telling the agent to read and follow the skill file.

```shell
acp \
  --agent claude \
  --provider vertex-ai \
  --skill-path /path/to/skills \
  --prompt "Execute the **java-project-discovery** skill."
```

Using an environment variable:
```shell
export SKILL_PATH=/path/to/skills
acp \
  --agent claude \
  --provider vertex-ai \
  --prompt "Execute the **java-project-discovery** skill."
```

## Using --workspace-path

The `--workspace-path` option (or `WORKSPACE_PATH` env var) sets the project directory used as CWD for the agent session. If not specified, it defaults to the directory where the command is executed.

```shell
acp \
  --agent claude \
  --provider vertex-ai \
  --workspace-path /path/to/my-project \
  --prompt "Say Hello"
```

Using an environment variable:
```shell
export WORKSPACE_PATH=/path/to/my-project
acp \
  --agent claude \
  --provider vertex-ai \
  --prompt "Say Hello"
```

## Using --backup and --backup-project-name

The `--backup` option (`-b`) creates a copy of the workspace under `target/workdirs/` before the agent runs. The `--backup-project-name` option controls the directory name used in the backup. When backup succeeds, the session CWD is automatically set to the backup directory.

```shell
acp \
  --agent claude \
  --provider vertex-ai \
  --backup yes \
  --backup-project-name my-todo-app \
  --prompt "Refactor the REST endpoints to use Quarkus REST."
```

Combining `--workspace-path` with backup:
```shell
acp \
  --agent claude \
  --provider vertex-ai \
  --workspace-path /path/to/my-project \
  --backup yes \
  --prompt "Refactor the service layer"
```

To disable backup:
```shell
acp \
  --agent claude \
  --provider vertex-ai \
  --backup no \
  --prompt "Say Hello"
```

## Using --log-level

The `--log-level` option (`-l`) controls log verbosity. Use `DEBUG` to see agent thoughts, tool calls, and usage details. Use `TRACE` to see raw JSON-RPC messages.

```shell
acp \
  --agent claude \
  --provider vertex-ai \
  -l DEBUG \
  --prompt "Review the pom.xml and propose improvements"
```

```shell
acp \
  --agent claude \
  --provider vertex-ai \
  -l TRACE \
  --prompt "Say Hello"
```
