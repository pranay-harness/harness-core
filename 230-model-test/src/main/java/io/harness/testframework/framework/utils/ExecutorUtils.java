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
  private static String BAZEL_BIN_PATH = null;
  static {
    log.info("initializing bazel bin path");
    try {
//      BAZEL_BIN_PATH = getBazelBinPath(null);
      log.info("BAZEL_BIN_PATH set to {}", BAZEL_BIN_PATH);
    } catch (Exception e) {
      log.error("Failed to set BAZEL_BIN_PATH", e);
    }
  }

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
    log.info("Fetching bazel bin path");
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
    return Paths.get(getBazelBinPathCached(), moduleName, jarFileName);
  }

  private static String getBazelBinPathCached() {
//    return getBazelBinPath(null);
    if (null != BAZEL_BIN_PATH) {
      return BAZEL_BIN_PATH;
    } else {
      log.info("Failed to init bazel bin path");
      return "/tmp/execroot/harness_monorepo/bazel-out/k8-fastbuild/bin";
    }
  }

  public static void addJar(Path jar, List<String> command) {
    command.add("-jar");
    command.add(jar.toString());
  }

  public static void addConfig(Path config, List<String> command) {
    command.add(config.toString());
  }
}
