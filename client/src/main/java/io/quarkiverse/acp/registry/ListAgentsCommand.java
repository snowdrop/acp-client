package io.quarkiverse.acp.registry;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand that lists ACP agents -- either locally installed or from the
 * remote registry.
 *
 * <p>Usage:
 * <pre>{@code
 * acp reg list              # installed agents
 * acp reg list --registry   # all agents from the remote ACP registry
 * }</pre>
 */
@CommandDefinition(
        name = "list",
        description = "List ACP agents (installed or from the registry)"
)
public class ListAgentsCommand implements Command<CommandInvocation> {

    @Option(shortName = 'r', name = "registry", hasValue = false,
            description = "List all agents from the remote ACP registry instead of installed ones")
    boolean fromRegistry;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        var manager = new AcpRegistryManager();
        try {
            if (fromRegistry) {
                listRegistryAgents(manager, invocation);
            } else {
                listInstalledAgents(manager, invocation);
            }
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            invocation.println("Error: " + e.getMessage());
            return CommandResult.FAILURE;
        }
    }

    // -- Registry listing ----

    private void listRegistryAgents(AcpRegistryManager manager, CommandInvocation invocation) throws Exception {
        var registry = manager.fetchRegistry();
        String platform = AcpRegistryManager.detectPlatform();

        invocation.println("ACP Registry v" + registry.version()
                + " - " + registry.agents().size() + " agents available");
        invocation.println("Current platform: " + platform);
        invocation.println("");
        invocation.println(String.format("%-25s %-12s %-14s %s", "ID", "VERSION", "DISTRIBUTION", "DESCRIPTION"));
        invocation.println("-".repeat(90));

        for (var agent : registry.agents()) {
            String distType = describeDistribution(agent, platform);
            boolean installed = manager.isInstalled(agent.id());
            String desc = agent.description() != null ? truncate(agent.description(), 38) : "";
            String idDisplay = agent.website() != null && !agent.website().isEmpty()
                    ? "\033]8;;" + agent.website() + "\033\\" + agent.id() + "\033]8;;\033\\"
                    : agent.id();
            int padding = 25 - agent.id().length();
            invocation.println(String.format("%s%-" + padding + "s %-12s %-14s %s%s",
                    idDisplay,
                    "",
                    agent.version(),
                    distType,
                    desc,
                    installed ? " [installed]" : ""));
        }
    }

    // -- Installed listing ----

    private void listInstalledAgents(AcpRegistryManager manager, CommandInvocation invocation) {
        var installed = manager.listInstalled();
        if (installed.isEmpty()) {
            invocation.println("No ACP agents installed in " + AcpRegistryManager.ACP_HOME);
            invocation.println("");
            invocation.println("  acp reg list --registry   list available agents");
            invocation.println("  acp reg install <id>      install an agent");
            return;
        }

        invocation.println("Installed ACP agents (" + AcpRegistryManager.ACP_HOME + "):");
        invocation.println("");
        invocation.println(String.format("%-25s %-12s %-8s %-18s %s",
                "ID", "VERSION", "TYPE", "PLATFORM", "COMMAND"));
        invocation.println("-".repeat(95));

        for (var agent : installed) {
            invocation.println(String.format("%-25s %-12s %-8s %-18s %s",
                    agent.id(),
                    agent.version(),
                    agent.distributionType(),
                    agent.platform(),
                    truncate(agent.cmd(), 30)));
        }
    }

    // -- Helpers ----

    private static String describeDistribution(AcpRegistryManager.Agent agent, String platform) {
        var dist = agent.distribution();
        if (dist == null) return "none";

        List<String> types = new ArrayList<>();
        if (dist.hasBinary()) {
            types.add(dist.binary().containsKey(platform) ? "binary" : "binary(*)");
        }
        if (dist.hasNpx()) types.add("npx");
        if (dist.hasUvx()) types.add("uvx");
        return String.join(", ", types);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
