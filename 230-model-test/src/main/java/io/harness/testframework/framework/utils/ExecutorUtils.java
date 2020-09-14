package io.harness.testframework.framework.utils;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.List;

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

  public static void addJar(Path jar, List<String> command) {
    command.add("-jar");
    command.add(jar.toString());
  }

  public static void addConfig(Path config, List<String> command) {
    command.add(config.toString());
  }
}
