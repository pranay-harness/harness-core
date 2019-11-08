package io.harness.testframework.framework.utils;

import java.nio.file.Path;
import java.util.List;

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

  public static void addJarConfig(Path jar, Path config, List<String> command) {
    command.add("-jar");
    command.add(jar.toString());
    command.add(config.toString());
  }
}
