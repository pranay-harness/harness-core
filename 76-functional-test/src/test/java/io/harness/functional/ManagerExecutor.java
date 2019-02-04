package io.harness.functional;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;

import com.google.inject.Singleton;

import io.fabric8.utils.Strings;
import io.harness.filesystem.FileIo;
import io.harness.resource.Project;
import io.harness.threading.Puller;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Singleton
public class ManagerExecutor {
  private static boolean failedAlready;
  private static final Logger logger = LoggerFactory.getLogger(ManagerExecutor.class);
  private static final String alpnJar =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar";

  public static void ensureManager() throws IOException {
    if (!isHealthy()) {
      executeLocalManager();
    }
  }

  private static void executeLocalManager() throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(AbstractFunctionalTest.class);
    final File directory = new File(directoryPath);
    final File lockfile = new File(directoryPath, "manager");

    if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
      try {
        if (isHealthy()) {
          return;
        }
        logger.info("Execute the manager from {}", directory);
        final Path jar = Paths.get(directory.getPath(), "71-rest", "target", "rest-capsule.jar");
        final Path config = Paths.get(directory.getPath(), "71-rest", "config.yml");
        String alpn = System.getProperty("user.home") + "/.m2/repository/" + alpnJar;

        if (!new File(alpn).exists()) {
          // if maven repo is not in the home dir, this might be a jenkins job, check in the special location.
          alpn = "/home/jenkins/maven-repositories/0/" + alpnJar;
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

        Puller.pullFor(ofMinutes(2), () -> isHealthy());
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

      given().config(config).when().get("/version").then().statusCode(200);
    } catch (Exception exception) {
      if (exception.getMessage().equals(previous.getMessage())) {
        logger.info("not healthy");
      } else {
        logger.info("not healthy", exception);
        previous = exception;
      }
      return false;
    }
    logger.info("healthy");
    return true;
  }
}
