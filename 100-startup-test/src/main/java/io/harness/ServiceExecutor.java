package io.harness;

import static io.harness.testframework.framework.utils.ExecutorUtils.addConfig;
import static io.harness.testframework.framework.utils.ExecutorUtils.addGCVMOptions;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJacocoAgentVM;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJar;
import static io.harness.testframework.framework.utils.ExecutorUtils.getConfig;
import static io.harness.testframework.framework.utils.ExecutorUtils.getJar;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static java.time.Duration.ofSeconds;

import io.harness.filesystem.FileIo;
import io.harness.project.Alpn;
import io.harness.resource.Project;
import io.harness.testframework.framework.Setup;
import io.harness.threading.Poller;

import io.fabric8.utils.Strings;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreConnectionPNames;
import org.zeroturnaround.exec.ProcessExecutor;

@Slf4j
public class ServiceExecutor {
  private String MODULE;
  private String CONFIG_YML;
  private boolean failedAlready;
  private Duration waiting;

  public ServiceExecutor(String module, String configYaml, Duration waiting) {
    this.MODULE = module;
    this.CONFIG_YML = configYaml;
    this.waiting = waiting;
  }

  public void ensureManager(Class<?> clazz) throws IOException {
    if (!isHealthy()) {
      executeLocalManager("server", clazz);
    }
  }

  private void executeLocalManager(String verb, Class<?> clazz) throws IOException {
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
        ProcessExecutor processExecutor = managerProcessExecutor(clazz, verb);
        processExecutor.start();

        Poller.pollFor(waiting, ofSeconds(2), this::isHealthy);
      } catch (RuntimeException | IOException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  private ProcessExecutor managerProcessExecutor(Class<?> clazz, String verb) {
    String directoryPath = Project.rootDirectory(clazz);
    final File directory = new File(directoryPath);

    log.info("Execute the manager from {}", directory);

    final Path jar = getJar(MODULE);
    final Path config = getConfig(directory.getAbsolutePath(), MODULE, CONFIG_YML);
    String alpn = Alpn.location();

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

  private Exception previous = new Exception();

  private boolean isHealthy() {
    try {
      RestAssuredConfig config =
          RestAssured.config().httpClient(httpClientConfig()
                                              .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
                                              .setParam(CoreConnectionPNames.SO_TIMEOUT, 5000));
      Setup.ci().config(config).when().get("/health").then().statusCode(HttpStatus.SC_OK);
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
