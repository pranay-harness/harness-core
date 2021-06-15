package io.harness.testframework.framework.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutorUtils {
  public static void addJacocoAgentVM(final Path jar, List<String> command) {
    final String jacocoAgentPath = System.getenv("JACOCO_AGENT_PATH");
    if (jacocoAgentPath == null) {
      return;
    }
    command.add(String.format(
        "-javaagent:%s=destfile=%s/jacoco-it.exec,output=file", jacocoAgentPath, jar.getParent().toAbsolutePath()));
  }

  public static void addGCVMOptions(List<String> command) {
    command.add("-Xmx4096m");
    command.add("-XX:+HeapDumpOnOutOfMemoryError");
    command.add("-XX:+PrintGCDetails");
    command.add("-XX:+PrintGCDateStamps");
    command.add("-Xloggc:mygclogfilename.gc");
    command.add("-XX:+UseParallelGC");
    command.add("-XX:MaxGCPauseMillis=500");
  }

  public static String getBazelBinPath() {
    String home = System.getProperty("user.home");
    if (home.contains("root")) {
      home = "/home/jenkins";
    }

    Path path = Paths.get(home, ".bazel-dirs", "bin");
    return path.toAbsolutePath().toString();
  }

  public static Path getJar(String moduleName) {
    return getJar(moduleName, "module_deploy.jar");
  }

  public static Path getConfig(String projectRootDirectory, String moduleName, String configFileName) {
    return Paths.get(projectRootDirectory, moduleName, configFileName);
  }

  public static Path getJar(String moduleName, String jarFileName) {
    return Paths.get(getBazelBinPath(), moduleName, jarFileName);
  }

  public static void addJar(Path jar, List<String> command) {
    command.add("-jar");
    command.add(jar.toString());
  }

  public static void addConfig(Path config, List<String> command) {
    command.add(config.toString());
  }
}
