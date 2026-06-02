package io.quarkiverse.acp.toolbox;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class ProjectUtil {

    private static final Logger logger = Logger.getLogger(ProjectUtil.class);

    /**
     *
     * Copy the application of the current working directory to a temporary folder
     *
     * @param name The name of the project/application to back up/duplicate
     * @param workDir The path of the cwd of the application
     * @return The temporary path of the project/application backuped
     */
    public static Path backupWorkspace(String name, Path workDir) {
        boolean isMaven = Files.exists(workDir.resolve("pom.xml"));
        boolean isGradle = Files.exists(workDir.resolve("build.gradle"))
                || Files.exists(workDir.resolve("build.gradle.kts"));

        if (!isMaven && !isGradle) {
            logger.info("Skipping workspace backup (not a Maven or Gradle project)");
            return null;
        }

        String projectName = ".".equals(name) ? workDir.getFileName().toString() : name;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        Set<String> excludes = Set.of("target", "build", ".git", ".gradle", ".idea", "node_modules",".claude", ".env");

        try {
            Path tempDirectory = Files.createTempDirectory(projectName + "-" + timestamp + "_");
            logger.debug("Temporary folder created successfully!");

            Files.walk(workDir)
                    .filter(path -> {
                        Path relative = workDir.relativize(path);
                        return relative.getNameCount() == 0
                                || !excludes.contains(relative.getName(0).toString());
                    })
                    .forEach(source -> {
                        Path dest = tempDirectory.resolve(workDir.relativize(source));
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
            logger.infof("Workspace backed up to: %s", tempDirectory);
            return tempDirectory;
        } catch (IOException e) {
            logger.errorf("Failed to backup workspace: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Resolve the value: command line value > environment variable > default value
     *
     * @param cliValue The command line value
     * @param envVar The environment variable name that we use to the value using: System.getenv(envVar);
     * @param defaultValue The default value
     * @return The value resolved
     */
    public static String resolveValueWithPrecedence(String cliValue, String envVar, String defaultValue) {
        if (cliValue != null && !cliValue.isEmpty()) {
            return cliValue;
        }
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return defaultValue;
    }

    public static void requireEnv(String varName, String provider) {
        String value = System.getenv(varName);
        if (value == null || value.isBlank()) {
            System.err.println("ERROR: " + varName + " environment variable is not set (required for " + provider + " provider).");
            System.exit(1);
        }
    }
}
