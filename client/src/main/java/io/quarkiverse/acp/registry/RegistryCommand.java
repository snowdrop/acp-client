package io.quarkiverse.acp.registry;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;

/**
 * Parent subcommand grouping ACP registry operations.
 *
 * <p>Usage:
 * <pre>{@code
 * acp reg list                # installed agents
 * acp reg list --registry     # remote registry
 * acp reg install opencode    # install an agent
 * acp reg remove opencode     # remove an installed agent
 * }</pre>
 */
@GroupCommandDefinition(
        name = "reg",
        description = "Manage ACP agents via the registry (list, install, remove)",
        groupCommands = {ListAgentsCommand.class, InstallCommand.class, RemoveCommand.class}
)
public class RegistryCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Usage: acp reg <command>");
        invocation.println("");
        invocation.println("Commands:");
        invocation.println("  list      List ACP agents (installed or from the registry)");
        invocation.println("  install   Install an ACP agent from the registry");
        invocation.println("  remove    Remove an installed ACP agent");
        return CommandResult.SUCCESS;
    }
}
