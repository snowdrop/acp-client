package io.quarkiverse.acp.registry;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

/**
 * Subcommand that installs an ACP agent from the remote registry.
 *
 * <p>Usage:
 * <pre>{@code
 * acp reg install opencode
 * acp reg install claude-acp --force
 * }</pre>
 *
 * <p>The agent binary (or npx/uvx metadata) is stored under
 * {@code $HOME/.acp/agents/<agent-id>/}.
 */
@CommandDefinition(
        name = "install",
        description = "Install an ACP agent from the registry"
)
public class InstallCommand implements Command<CommandInvocation> {

    @Argument(description = "Agent ID from the ACP registry (e.g. opencode, claude-acp, gemini)", required = true)
    String agentId;

    @Option(shortName = 'f', name = "force", hasValue = false,
            description = "Force reinstall even if the agent is already installed")
    boolean force;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        try {
            var manager = new AcpRegistryManager();
            manager.installAgent(agentId, force);
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            invocation.println("Error installing agent: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }
}
