package io.harness.testframework.framework;

import static io.harness.testframework.framework.utils.ExecutorUtils.addConfig;
import static io.harness.testframework.framework.utils.ExecutorUtils.addGCVMOptions;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJacocoAgentVM;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJar;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.filesystem.FileIo;
import io.harness.resource.Project;
import io.harness.testframework.framework.utils.FileUtils;
import io.harness.threading.Poller;

import com.google.inject.Singleton;
import io.fabric8.utils.Strings;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreConnectionPNames;
import org.zeroturnaround.exec.ProcessExecutor;

@Singleton
@Slf4j
public class ManagerExecutor {
  private static boolean failedAlready;
  private static Duration waiting = ofMinutes(5);

  public static void ensureManager(Class clazz, String alpnPath, String alpnJarPath) throws IOException {
    if (!isHealthy()) {
      final Path config = Paths.get(Project.rootDirectory(clazz), "360-cg-manager", "config.yml");
      FileUtils.modifyConfigFile(new File(config.toString()));
      executeLocalManager("server", clazz, alpnPath, alpnJarPath);
    }
  }

  public static void executeLocalManager(String verb, Class clazz, String alpnPath, String alpnJarPath)
      throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(clazz);
    final File lockfile = new File(directoryPath, "manager");

    if (FileIo.acquireLock(lockfile, waiting)) {
      try {
        if (isHealthy()) {
          return;
        }
        ProcessExecutor processExecutor = managerProcessExecutor(clazz, verb, alpnPath, alpnJarPath);
        processExecutor.start();

        Poller.pollFor(waiting, ofSeconds(2), ManagerExecutor::isHealthy);
      } catch (RuntimeException | IOException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  public static ProcessExecutor managerProcessExecutor(Class clazz, String verb, String alpnPath, String alpnJarPath) {
    String directoryPath = Project.rootDirectory(clazz);
    final File directory = new File(directoryPath);

    log.info("Execute the manager from {}", directory);

    final Path jar = Paths.get(System.getProperty("user.home") + "/.m2/repository/"
        + "software/wings/360-cg-manager/0.0.1-SNAPSHOT/360-cg-manager-0.0.1-SNAPSHOT-capsule.jar");

    final Path config = Paths.get(directory.getPath(), "360-cg-manager", "modified_config.yml");

    String alpn = System.getProperty("user.home") + "/.m2/repository/" + alpnJarPath;

    if (!new File(alpn).exists()) {
      // if maven repo is not in the home dir, this might be a jenkins job, check in the special location.
      alpn = alpnPath + alpnJarPath;
      if (!new File(alpn).exists()) {
        throw new RuntimeException("Missing alpn file");
      }
    }

    for (int i = 0; i < 10; i++) {
      log.info("***");
    }

    List<String> command = new ArrayList<>();
    command.add("java");
    command.add("-Xms1024m");

    addGCVMOptions(command);

    command.add("-Dfile.encoding=UTF-8");
    command.add("-Xbootclasspath/p:" + alpn);

    addJacocoAgentVM(jar, command);

    addJar(jar, command);
    command.add(verb);
    addConfig(config, command);

    log.info(Strings.join(command, " "));

    ProcessExecutor processExecutor = new ProcessExecutor();
    processExecutor.directory(directory);
    processExecutor.command(command);

    processExecutor.redirectOutput(System.out);
    processExecutor.redirectError(System.err);
    return processExecutor;
  }

  private static Exception previous = new Exception();

  private static boolean isHealthy() {
    try {
      RestAssuredConfig config =
          RestAssured.config().httpClient(httpClientConfig()
                                              .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
                                              .setParam(CoreConnectionPNames.SO_TIMEOUT, 5000));

      Setup.portal().config(config).when().get("/health").then().statusCode(HttpStatus.SC_OK);
    } catch (Exception exception) {
      if (exception.getMessage().equals(previous.getMessage())) {
        log.info("not healthy");
      } else {
        log.info("not healthy - {}", exception.getMessage());
        previous = exception;
      }
      return false;
    }
    log.info("healthy");
    return true;
  }
}
