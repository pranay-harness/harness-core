package io.harness.testframework.framework.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
@Slf4j
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

  public static String getBazelBinPath(File file) {
    Process processFinal = null;
    try {
      String rc = file == null ? "--noworkspace_rc" : "";
      processFinal = Runtime.getRuntime().exec(String.format("bazel %s info bazel-bin", rc), null, file);
      if (processFinal.waitFor() == 0) {
        try (InputStream inputStream = processFinal.getInputStream()) {
          BufferedReader processStdErr = new BufferedReader(new InputStreamReader(inputStream));
          return processStdErr.readLine();
        }
      } else {
        try (InputStream inputStream = processFinal.getErrorStream()) {
          Pattern pattern = Pattern.compile("ERROR: .* The pertinent workspace directory is: '(.*?)'");

          BufferedReader processStdErr = new BufferedReader(new InputStreamReader(inputStream));

          String error = "";
          String line;
          while ((line = processStdErr.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find() && file == null) {
              return getBazelBinPath(new File(matcher.group(1)));
            }
            error += line;
          }
          throw new RuntimeException(error);
        }
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      if (processFinal != null) {
        processFinal.destroyForcibly();
      }
    }
  }

  public static Path getJar(String moduleName) {
    return getJar(moduleName, "module_deploy.jar");
  }

  public static Path getConfig(String projectRootDirectory, String moduleName, String configFileName) {
    return Paths.get(projectRootDirectory, moduleName, configFileName);
  }

  public static Path getJar(String moduleName, String jarFileName) {
    return Paths.get(getBazelBinPath(null), moduleName, jarFileName);
  }

  public static void addJar(Path jar, List<String> command) {
    command.add("-jar");
    command.add(jar.toString());
  }

  public static void addConfig(Path config, List<String> command) {
    command.add(config.toString());
  }
}
