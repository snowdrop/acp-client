package io.smallrye.acp.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the ACP agent registry: fetching, caching, installing and resolving agents.
 *
 * <p>Agents are installed under {@code $HOME/.acp/agents/<agent-id>/}.
 * A cached copy of the remote registry is kept at {@code $HOME/.acp/registry.json}.
 */
public class AcpRegistryManager {

    private static final Logger logger = Logger.getLogger(AcpRegistryManager.class);

    public static final String REGISTRY_URL =
            "https://cdn.agentclientprotocol.com/registry/v1/latest/registry.json";

    public static final Path ACP_HOME = Path.of(System.getProperty("user.home"), ".acp");
    public static final Path AGENTS_DIR = ACP_HOME.resolve("agents");
    public static final Path REGISTRY_CACHE = ACP_HOME.resolve("registry.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ── Model records ───────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Registry(String version, List<Agent> agents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Agent(
            String id,
            String name,
            String version,
            String description,
            String repository,
            String website,
            List<String> authors,
            String license,
            String icon,
            Distribution distribution
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Distribution(
            NpxInfo npx,
            UvxInfo uvx,
            Map<String, PlatformBinary> binary
    ) {
        public boolean hasBinary() { return binary != null && !binary.isEmpty(); }
        public boolean hasNpx()    { return npx != null && npx.packageName() != null; }
        public boolean hasUvx()    { return uvx != null && uvx.packageName() != null; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NpxInfo(
            @JsonProperty("package") String packageName,
            List<String> args,
            Map<String, String> env
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UvxInfo(
            @JsonProperty("package") String packageName,
            List<String> args
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlatformBinary(
            String archive,
            String cmd,
            List<String> args,
            Map<String, String> env
    ) {}

    /**
     * Metadata persisted at {@code $HOME/.acp/agents/<id>/agent.json}
     * after an agent is installed.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstalledAgent(
            String id,
            String name,
            String version,
            String platform,
            String distributionType,   // "binary", "npx", "uvx"
            String cmd,                // absolute binary path, or "npx" / "uvx"
            List<String> args,
            String npxPackage,
            String uvxPackage,
            String installedAt
    ) {}

    /** Resolved command ready to be passed to {@code AgentParameters}. */
    public record AgentCommand(String binary, List<String> args) {}

    // ── Registry operations ─────────────────────────────────────────────────

    /**
     * Fetches the remote registry and caches it locally.
     */
    public Registry fetchRegistry() throws IOException, InterruptedException {
        logger.info("Fetching ACP registry from " + REGISTRY_URL);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REGISTRY_URL))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch registry: HTTP " + response.statusCode());
        }

        String json = response.body();
        Files.createDirectories(ACP_HOME);
        Files.writeString(REGISTRY_CACHE, json);
        logger.infof("Registry cached at %s", REGISTRY_CACHE);

        return MAPPER.readValue(json, Registry.class);
    }

    /**
     * Loads the locally cached registry, or {@code null} if no cache exists.
     */
    public Registry getCachedRegistry() {
        if (!Files.exists(REGISTRY_CACHE)) {
            return null;
        }
        try {
            return MAPPER.readValue(REGISTRY_CACHE.toFile(), Registry.class);
        } catch (IOException e) {
            logger.warnf("Failed to read cached registry: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Returns the cached registry if available, otherwise fetches from remote.
     */
    public Registry getOrFetchRegistry() throws IOException, InterruptedException {
        Registry cached = getCachedRegistry();
        return cached != null ? cached : fetchRegistry();
    }

    /**
     * Finds an agent by ID in the given registry.
     */
    public Agent findAgent(Registry registry, String agentId) {
        if (registry == null || registry.agents() == null) return null;
        return registry.agents().stream()
                .filter(a -> a.id().equals(agentId))
                .findFirst()
                .orElse(null);
    }

    // ── Platform detection ──────────────────────────────────────────────────

    /**
     * Detects the current platform key (e.g. {@code darwin-aarch64}, {@code linux-x86_64}).
     */
    public static String detectPlatform() {
        String os   = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        String osKey;
        if (os.contains("mac") || os.contains("darwin")) {
            osKey = "darwin";
        } else if (os.contains("linux")) {
            osKey = "linux";
        } else if (os.contains("win")) {
            osKey = "windows";
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }

        String archKey;
        if ("aarch64".equals(arch) || "arm64".equals(arch)) {
            archKey = "aarch64";
        } else if ("x86_64".equals(arch) || "amd64".equals(arch)) {
            archKey = "x86_64";
        } else {
            throw new RuntimeException("Unsupported architecture: " + arch);
        }

        return osKey + "-" + archKey;
    }

    // ── Install operations ──────────────────────────────────────────────────

    /**
     * Installs (or reinstalls) an agent from the remote ACP registry.
     *
     * @param agentId the registry agent ID (e.g. {@code opencode}, {@code claude-acp})
     * @param force   if {@code true}, reinstall even if already present
     */
    public void installAgent(String agentId, boolean force) throws IOException, InterruptedException {
        Registry registry = fetchRegistry();
        Agent agent = findAgent(registry, agentId);

        if (agent == null) {
            System.err.println("Agent '" + agentId + "' not found in the ACP registry.");
            System.err.println();
            System.err.println("Run:  acp reg list --registry   to see available agents.");
            return;
        }

        Path agentDir = AGENTS_DIR.resolve(agentId);

        if (Files.exists(agentDir) && !force) {
            InstalledAgent existing = getInstalledAgent(agentId);
            if (existing != null) {
                System.out.println("Agent '" + agentId + "' is already installed (version "
                        + existing.version() + ").");
                System.out.println("Use --force to reinstall.");
                return;
            }
        }

        if (Files.exists(agentDir) && force) {
            deleteDirectory(agentDir);
        }
        Files.createDirectories(agentDir);

        String platform = detectPlatform();
        Distribution dist = agent.distribution();

        if (dist == null) {
            System.err.println("Agent '" + agentId + "' has no distribution information.");
            return;
        }

        // Prefer binary distribution for current platform
        if (dist.hasBinary()) {
            PlatformBinary platformBinary = dist.binary().get(platform);
            if (platformBinary != null) {
                installBinaryAgent(agent, platformBinary, platform, agentDir);
                return;
            }
            logger.warnf("No binary for platform '%s'. Checking npx/uvx fallback...", platform);
        }

        if (dist.hasNpx()) {
            installNpxAgent(agent, agentDir);
            return;
        }

        if (dist.hasUvx()) {
            installUvxAgent(agent, agentDir);
            return;
        }

        System.err.println("No suitable distribution found for agent '"
                + agentId + "' on platform '" + platform + "'.");
    }

    private void installBinaryAgent(Agent agent, PlatformBinary platformBinary,
                                    String platform, Path agentDir)
            throws IOException, InterruptedException {

        String archiveUrl = platformBinary.archive();
        System.out.println("Downloading " + agent.name() + " v" + agent.version()
                + " for " + platform + " ...");
        System.out.println("  URL: " + archiveUrl);

        // Download
        Path archiveFile = downloadFile(archiveUrl, agentDir);
        System.out.println("  Downloaded: " + archiveFile.getFileName());

        // Extract
        System.out.println("  Extracting ...");
        extractArchive(archiveFile, agentDir);

        // Resolve cmd path (strip leading ./)
        String cmd = platformBinary.cmd();
        if (cmd.startsWith("./")) {
            cmd = cmd.substring(2);
        }
        Path binaryPath = agentDir.resolve(cmd);

        // Make executable on Unix
        makeExecutable(binaryPath);

        // If the binary is not at the expected path, search for it
        if (!Files.exists(binaryPath)) {
            logger.warnf("Expected binary not found at %s — searching...", binaryPath);
            String cmdName = Path.of(cmd).getFileName().toString();
            try (Stream<Path> walk = Files.walk(agentDir)) {
                Optional<Path> found = walk
                        .filter(p -> p.getFileName().toString().equals(cmdName))
                        .filter(p -> !p.toString().endsWith(".json"))
                        .findFirst();
                if (found.isPresent()) {
                    binaryPath = found.get();
                    cmd = agentDir.relativize(binaryPath).toString();
                    System.out.println("  Found binary at: " + binaryPath);
                    makeExecutable(binaryPath);
                }
            }
        }

        // Save metadata
        List<String> args = platformBinary.args() != null ? platformBinary.args() : List.of();
        InstalledAgent installed = new InstalledAgent(
                agent.id(), agent.name(), agent.version(), platform,
                "binary",
                agentDir.resolve(cmd).toAbsolutePath().toString(),
                args, null, null,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        saveInstalledAgent(agent.id(), installed);

        System.out.println("  Installed '" + agent.id() + "' v" + agent.version());
        System.out.println("  Binary: " + installed.cmd());
    }

    private void installNpxAgent(Agent agent, Path agentDir)
            throws IOException, InterruptedException {

        NpxInfo npx = agent.distribution().npx();
        String packageName = npx.packageName();
        System.out.println("Installing npx agent: " + agent.name() + " v" + agent.version());
        System.out.println("  Package: " + packageName);

        // Run npm install into the agent directory
        System.out.println("  Running: npm install " + packageName + " ...");
        runProcess(agentDir, "npm", "install", "--prefix", agentDir.toString(), packageName);

        // Resolve the binary name from the package
        // e.g. @agentclientprotocol/claude-agent-acp@0.37.0 → claude-agent-acp
        String binName = resolveBinName(packageName);
        Path binPath = agentDir.resolve("node_modules").resolve(".bin").resolve(binName);

        if (!Files.exists(binPath)) {
            // Fallback: search in .bin for any executable
            Path binDir = agentDir.resolve("node_modules").resolve(".bin");
            if (Files.exists(binDir)) {
                try (Stream<Path> bins = Files.list(binDir)) {
                    Optional<Path> found = bins.filter(Files::isExecutable).findFirst();
                    if (found.isPresent()) {
                        binPath = found.get();
                        binName = binPath.getFileName().toString();
                        System.out.println("  Resolved binary: " + binName);
                    }
                }
            }
        }

        List<String> args = npx.args() != null ? npx.args() : List.of();
        InstalledAgent installed = new InstalledAgent(
                agent.id(), agent.name(), agent.version(), detectPlatform(),
                "npx",
                binPath.toAbsolutePath().toString(),
                args, packageName, null,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        saveInstalledAgent(agent.id(), installed);

        System.out.println("  Installed '" + agent.id() + "' v" + agent.version());
        System.out.println("  Binary: " + binPath.toAbsolutePath());
    }

    private void installUvxAgent(Agent agent, Path agentDir)
            throws IOException, InterruptedException {

        UvxInfo uvx = agent.distribution().uvx();
        String packageName = uvx.packageName();
        System.out.println("Installing uvx agent: " + agent.name() + " v" + agent.version());
        System.out.println("  Package: " + packageName);

        // Run uv tool install into a venv inside the agent directory
        Path venvDir = agentDir.resolve("venv");
        System.out.println("  Running: uv pip install " + packageName + " ...");
        runProcess(agentDir, "uv", "venv", venvDir.toString());
        runProcess(agentDir, "uv", "pip", "install",
                "--python", venvDir.resolve("bin").resolve("python").toString(),
                packageName);

        // Resolve the binary name from the package
        String binName = resolveBinName(packageName);
        Path binPath = venvDir.resolve("bin").resolve(binName);

        if (!Files.exists(binPath)) {
            // Fallback: search in venv/bin for any executable (excluding python/pip)
            Path binDir = venvDir.resolve("bin");
            if (Files.exists(binDir)) {
                try (Stream<Path> bins = Files.list(binDir)) {
                    Optional<Path> found = bins
                            .filter(Files::isExecutable)
                            .filter(p -> {
                                String n = p.getFileName().toString();
                                return !n.startsWith("python") && !n.startsWith("pip")
                                        && !n.equals("activate");
                            })
                            .findFirst();
                    if (found.isPresent()) {
                        binPath = found.get();
                        binName = binPath.getFileName().toString();
                        System.out.println("  Resolved binary: " + binName);
                    }
                }
            }
        }

        List<String> args = uvx.args() != null ? uvx.args() : List.of();
        InstalledAgent installed = new InstalledAgent(
                agent.id(), agent.name(), agent.version(), detectPlatform(),
                "uvx",
                binPath.toAbsolutePath().toString(),
                args, null, packageName,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        saveInstalledAgent(agent.id(), installed);

        System.out.println("  Installed '" + agent.id() + "' v" + agent.version());
        System.out.println("  Binary: " + binPath.toAbsolutePath());
    }

    /**
     * Extracts a binary name from a package specifier.
     * <ul>
     *   <li>{@code @agentclientprotocol/claude-agent-acp@0.37.0} &rarr; {@code claude-agent-acp}</li>
     *   <li>{@code pi-acp@0.0.27} &rarr; {@code pi-acp}</li>
     *   <li>{@code fast-agent-acp==0.7.12} &rarr; {@code fast-agent-acp}</li>
     * </ul>
     */
    private static String resolveBinName(String packageSpec) {
        String name = packageSpec;
        // Strip version: @0.37.0 or ==0.7.12
        int atVersion = name.lastIndexOf('@');
        // For scoped packages like @scope/name@version, the last @ is the version separator
        if (atVersion > 0) {
            name = name.substring(0, atVersion);
        }
        int eqVersion = name.indexOf("==");
        if (eqVersion > 0) {
            name = name.substring(0, eqVersion);
        }
        // Strip scope: @agentclientprotocol/
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name;
    }

    // ── Download & extract ──────────────────────────────────────────────────

    private Path downloadFile(String url, Path targetDir) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Download failed: HTTP " + response.statusCode() + " from " + url);
        }

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf('?'));
        }
        Path archiveFile = targetDir.resolve(fileName);

        try (InputStream in = response.body()) {
            Files.copy(in, archiveFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return archiveFile;
    }

    private void extractArchive(Path archiveFile, Path targetDir)
            throws IOException, InterruptedException {

        String name = archiveFile.getFileName().toString().toLowerCase();

        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            runProcess(targetDir, "tar", "xzf", archiveFile.toString(), "-C", targetDir.toString());
        } else if (name.endsWith(".tar.xz")) {
            runProcess(targetDir, "tar", "xJf", archiveFile.toString(), "-C", targetDir.toString());
        } else if (name.endsWith(".zip")) {
            extractZip(archiveFile, targetDir);
        } else {
            throw new IOException("Unsupported archive format: " + name);
        }
    }

    private void extractZip(Path archiveFile, Path targetDir) throws IOException {
        try (FileSystem zipFs = FileSystems.newFileSystem(archiveFile, Map.of())) {
            for (Path root : zipFs.getRootDirectories()) {
                try (Stream<Path> stream = Files.walk(root)) {
                    stream.forEach(source -> {
                        try {
                            Path dest = targetDir.resolve(root.relativize(source).toString());
                            if (Files.isDirectory(source)) {
                                Files.createDirectories(dest);
                            } else {
                                Files.createDirectories(dest.getParent());
                                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            }
        }
    }

    // ── Installed-agent operations ──────────────────────────────────────────

    /**
     * Reads the metadata for an installed agent, or {@code null} if not installed.
     */
    public InstalledAgent getInstalledAgent(String agentId) {
        Path metadataFile = AGENTS_DIR.resolve(agentId).resolve("agent.json");
        if (!Files.exists(metadataFile)) {
            return null;
        }
        try {
            return MAPPER.readValue(metadataFile.toFile(), InstalledAgent.class);
        } catch (IOException e) {
            logger.warnf("Failed to read metadata for '%s': %s", agentId, e.getMessage());
            return null;
        }
    }

    /**
     * Lists all locally installed agents.
     */
    public List<InstalledAgent> listInstalled() {
        if (!Files.exists(AGENTS_DIR)) {
            return List.of();
        }
        List<InstalledAgent> result = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(AGENTS_DIR)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                InstalledAgent a = getInstalledAgent(dir.getFileName().toString());
                if (a != null) result.add(a);
            });
        } catch (IOException e) {
            logger.warnf("Failed to list installed agents: %s", e.getMessage());
        }
        return result;
    }

    public boolean isInstalled(String agentId) {
        return getInstalledAgent(agentId) != null;
    }

    /**
     * Removes an installed agent by deleting its directory under
     * {@code $HOME/.acp/agents/<agent-id>/}.
     */
    public void removeAgent(String agentId) throws IOException {
        InstalledAgent installed = getInstalledAgent(agentId);
        if (installed == null) {
            System.err.println("Agent '" + agentId + "' is not installed.");
            System.err.println();
            System.err.println("Run:  acp reg list   to see installed agents.");
            return;
        }

        Path agentDir = AGENTS_DIR.resolve(agentId);
        deleteDirectory(agentDir);
        System.out.println("Agent '" + agentId + "' (v" + installed.version() + ") removed.");
    }

    private void saveInstalledAgent(String agentId, InstalledAgent installed) throws IOException {
        Path metadataFile = AGENTS_DIR.resolve(agentId).resolve("agent.json");
        Files.createDirectories(metadataFile.getParent());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), installed);
    }

    // ── Agent resolution ────────────────────────────────────────────────────

    /**
     * Resolves the binary command and arguments for an installed agent.
     *
     * @return the resolved command, or {@code null} if the agent is not installed
     */
    public AgentCommand resolveAgentCommand(String agentId) {
        InstalledAgent installed = getInstalledAgent(agentId);
        if (installed == null) {
            return null;
        }

        // All distribution types now resolve to a local binary after install
        List<String> args = installed.args() != null ? installed.args() : List.of();
        return new AgentCommand(installed.cmd(), args);
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private void makeExecutable(Path path) throws IOException, InterruptedException {
        if (!System.getProperty("os.name").toLowerCase().contains("win") && Files.exists(path)) {
            runProcess(path.getParent(), "chmod", "+x", path.toString());
        }
    }

    private void runProcess(Path workDir, String... command)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .inheritIO()
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed (exit " + exitCode + "): "
                    + String.join(" ", command));
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
    }
}
