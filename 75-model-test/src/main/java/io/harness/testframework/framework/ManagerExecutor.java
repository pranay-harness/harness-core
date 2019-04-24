package io.harness.testframework.framework;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import com.google.inject.Singleton;

import io.fabric8.utils.Strings;
import io.harness.filesystem.FileIo;
import io.harness.resource.Project;
import io.harness.testframework.framework.utils.FileUtils;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.threading.Puller;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreConnectionPNames;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Singleton
@Slf4j
public class ManagerExecutor {
  private static boolean failedAlready;

  public static void ensureManager(Class clazz, String alpnPath, String alpnJarPath) throws IOException {
    if (!isHealthy()) {
      final Path config = Paths.get(Project.rootDirectory(clazz), "71-rest", "config.yml");
      FileUtils.modifySMTPInConfigFile(new File(config.toString()), TestUtils.getDefaultSmtpConfig());
      executeLocalManager(clazz, alpnPath, alpnJarPath);
    }
  }

  private static void executeLocalManager(Class clazz, String alpnPath, String alpnJarPath) throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(clazz);
    final File directory = new File(directoryPath);
    final File lockfile = new File(directoryPath, "manager");

    if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
      try {
        if (isHealthy()) {
          return;
        }
        logger.info("Execute the manager from {}", directory);
        final Path jar = Paths.get(directory.getPath(), "71-rest", "target", "rest-capsule.jar");
        final Path config = Paths.get(directory.getPath(), "71-rest", "modified_config.yml");
        String alpn = System.getProperty("user.home") + "/.m2/repository/" + alpnJarPath;

        if (!new File(alpn).exists()) {
          // if maven repo is not in the home dir, this might be a jenkins job, check in the special location.
          alpn = alpnPath + alpnJarPath;
          if (!new File(alpn).exists()) {
            throw new RuntimeException("Missing alpn file");
          }
        }

        for (int i = 0; i < 10; i++) {
          logger.info("***");
        }

        final List<String> command = asList("java", "-Xms1024m", "-Xmx4096m", "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps", "-Xloggc:mygclogfilename.gc", "-XX:+UseParallelGC",
            "-XX:MaxGCPauseMillis=500", "-Dfile.encoding=UTF-8", "-Xbootclasspath/p:" + alpn, "-jar", jar.toString(),
            config.toString());

        logger.info(Strings.join(command, " "));

        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.directory(directory);
        processExecutor.command(command);

        processExecutor.redirectOutput(System.out);
        processExecutor.redirectError(System.err);
        //        processExecutor.redirectOutput(null);
        //        processExecutor.redirectError(null);

        processExecutor.start();

        Puller.pullFor(ofMinutes(2), ofSeconds(2), () -> isHealthy());
      } catch (RuntimeException | IOException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
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
        logger.info("not healthy");
      } else {
        logger.info(format("not healthy - %s", exception.getMessage()));
        previous = exception;
      }
      return false;
    }
    logger.info("healthy");
    return true;
  }
}
