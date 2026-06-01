package io.quarkiverse.acp.toolbox;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class GitUtil {

    private static final Logger logger = Logger.getLogger(GitUtil.class);
    private static final Path SKILLS_DIR = Path.of(System.getProperty("user.home"), ".agents", "skills");

    public static boolean isUrl(String skillPath) {
        if (skillPath == null || skillPath.isBlank()) {
            return false;
        }
        return skillPath.startsWith("http://")
            || skillPath.startsWith("https://")
            || skillPath.startsWith("git://")
            || skillPath.startsWith("ssh://")
            || skillPath.matches("^[\\w.-]+@[\\w.-]+:.*");
    }

    /**
     * Parsed components of a GitHub {@code /tree/branch/subpath} URL.
     *
     * @param repoUrl  the repository clone URL (everything before {@code /tree/})
     * @param branch   the branch name (segment right after {@code /tree/})
     * @param subPath  the subdirectory path within the repo, or {@code null} if none
     */
    record GitHubTreeUrl(String repoUrl, String branch, String subPath) {}

    /**
     * Attempts to parse a GitHub "tree" URL into its components.
     *
     * <p>Example:
     * {@code https://github.com/org/repo/tree/main/path/to/dir}
     * → repoUrl={@code https://github.com/org/repo}, branch={@code main}, subPath={@code path/to/dir}
     *
     * @return the parsed components, or {@code null} if the URL does not match the pattern
     */
    static GitHubTreeUrl parseGitHubTreeUrl(String url) {
        String cleaned = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        int treeIdx = cleaned.indexOf("/tree/");
        if (treeIdx < 0) {
            return null;
        }
        String repoUrl = cleaned.substring(0, treeIdx);
        String rest = cleaned.substring(treeIdx + "/tree/".length());
        int slashIdx = rest.indexOf('/');
        if (slashIdx < 0) {
            // Just a branch, no subdirectory: e.g. /tree/main
            return new GitHubTreeUrl(repoUrl, rest, null);
        }
        String branch = rest.substring(0, slashIdx);
        String subPath = rest.substring(slashIdx + 1);
        return new GitHubTreeUrl(repoUrl, branch, subPath.isEmpty() ? null : subPath);
    }

    /**
     * Resolves a skill URL (HTTP, Git, or SCP-style) to a local path
     * under {@code $HOME/.agents/skills/<repo-name>}.
     *
     * <p>If the URL is a GitHub "tree" URL pointing to a subdirectory
     * (e.g. {@code https://github.com/org/repo/tree/main/sub/dir}),
     * only the repository is cloned and the returned path points to
     * the subdirectory within the checkout.
     *
     * <p>If the repository has already been cloned, it is pulled to get
     * the latest changes. Otherwise, a fresh clone is performed.
     *
     * @param url the remote URL pointing to the skill repository (or a subdirectory within it)
     * @return the local {@link Path} where the skill is located
     * @throws IOException if cloning/pulling or directory creation fails
     */
    public static Path resolveFromUrl(String url) throws IOException {
        GitHubTreeUrl parsed = parseGitHubTreeUrl(url);

        String repoUrl = parsed != null ? parsed.repoUrl : url;
        String repoName = extractRepoName(repoUrl);
        boolean hasSubPath = parsed != null && parsed.subPath != null;

        // When the URL points to a subdirectory, clone into a hidden .repos/
        // directory so that SKILLS_DIR stays flat with one entry per skill.
        Path cloneParent = hasSubPath ? SKILLS_DIR.resolve(".repos") : SKILLS_DIR;
        Path targetDir = cloneParent.resolve(repoName);

        Files.createDirectories(cloneParent);

        if (Files.isDirectory(targetDir.resolve(".git"))) {
            logger.infof("Skill repo already cloned at %s — pulling latest changes", targetDir);
            if (parsed != null && parsed.branch != null) {
                git(targetDir, "git", "checkout", parsed.branch);
            }
            git(targetDir, "git", "pull", "--ff-only");
        } else {
            logger.infof("Cloning skill repo %s into %s", repoUrl, targetDir);
            if (parsed != null && parsed.branch != null) {
                git(cloneParent, "git", "clone", "--branch", parsed.branch, repoUrl, repoName);
            } else {
                git(cloneParent, "git", "clone", repoUrl, repoName);
            }
        }

        if (hasSubPath) {
            Path subDir = targetDir.resolve(parsed.subPath);
            if (!Files.isDirectory(subDir)) {
                throw new IOException("Subdirectory not found in repository: " + parsed.subPath);
            }
            // Expose the skill under a flat name in SKILLS_DIR:
            //   ~/.agents/skills/<skill-name> -> .repos/<repo>/<sub/path>
            String skillName = Path.of(parsed.subPath).getFileName().toString();
            Path skillLink = SKILLS_DIR.resolve(skillName);

            if (Files.isSymbolicLink(skillLink)) {
                // Update symlink if target changed
                Path existingTarget = Files.readSymbolicLink(skillLink);
                if (!existingTarget.equals(subDir.toAbsolutePath())) {
                    Files.delete(skillLink);
                    Files.createSymbolicLink(skillLink, subDir.toAbsolutePath());
                }
            } else if (!Files.exists(skillLink, LinkOption.NOFOLLOW_LINKS)) {
                Files.createSymbolicLink(skillLink, subDir.toAbsolutePath());
                logger.infof("Created skill symlink: %s -> %s", skillLink, subDir);
            } else {
                // A real file/directory already exists with this name — use the actual path
                logger.warnf("Cannot create symlink %s (path already exists) — using full path", skillLink);
                return subDir;
            }
            return skillLink;
        }

        return targetDir;
    }

    /**
     * Extracts a short repository name from a URL.
     * <p>Examples:
     * <ul>
     *   <li>{@code https://github.com/org/my-skill.git} → {@code my-skill}</li>
     *   <li>{@code git@github.com:org/my-skill.git}     → {@code my-skill}</li>
     *   <li>{@code ssh://git@host/repo}                  → {@code repo}</li>
     * </ul>
     */
    static String extractRepoName(String url) {
        // For SCP-style URLs (git@host:org/repo.git), take the part after the last '/'
        // or after ':' if there is no '/'.
        String path = url;
        int colonIdx = path.indexOf(':');
        int slashAfterScheme = path.indexOf("://");
        if (colonIdx >= 0 && (slashAfterScheme < 0 || colonIdx != slashAfterScheme)) {
            // SCP-style: take everything after the colon
            path = path.substring(colonIdx + 1);
        }

        // Take the last path segment
        int lastSlash = path.lastIndexOf('/');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;

        // Strip trailing .git
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Cannot derive repository name from URL: " + url);
        }

        return name;
    }

    private static void git(Path workDir, String... command) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .inheritIO();
            int exitCode = pb.start().waitFor();
            if (exitCode != 0) {
                throw new IOException("Git command failed (exit " + exitCode + "): " + String.join(" ", command));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git command interrupted: " + String.join(" ", command), e);
        }
    }
}