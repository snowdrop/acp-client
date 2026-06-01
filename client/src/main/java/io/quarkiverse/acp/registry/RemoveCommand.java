package io.quarkiverse.acp.registry;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

/**
 * Subcommand that removes a previously installed ACP agent.
 *
 * <p>Usage:
 * <pre>{@code
 * acp reg remove opencode
 * }</pre>
 *
 * <p>Deletes the agent directory under {@code $HOME/.acp/agents/<agent-id>/}.
 */
@CommandDefinition(
        name = "remove",
        description = "Remove an installed ACP agent"
)
public class RemoveCommand implements Command<CommandInvocation> {

    @Argument(description = "Agent ID to remove (e.g. opencode, claude-acp)", required = true)
    String agentId;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        try {
            var manager = new AcpRegistryManager();
            manager.removeAgent(agentId);
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            invocation.println("Error removing agent: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }
}
