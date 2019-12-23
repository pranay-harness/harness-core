package io.harness.testframework.framework;

import static io.harness.testframework.framework.utils.ExecutorUtils.addGCVMOptions;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJacocoAgentVM;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJarConfig;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.fabric8.utils.Strings;
import io.grpc.Channel;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.harness.filesystem.FileIo;
import io.harness.resource.Project;
import io.harness.threading.Poller;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Start Event service as part of functional tests
 */
@UtilityClass
@Slf4j
public class EventServerExecutor {
  private static final String alpnJar =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar";
  private static final String alpn = "/home/jenkins/maven-repositories/0/";
  private static boolean failedAlready;

  @Getter(lazy = true) private static final Channel channel = makeChannel();

  public static void ensureEventServer(Class clazz, String alpnPath, String alpnJarPath) throws IOException {
    if (!isHealthy()) {
      executeLocalEventServer(clazz, alpnPath, alpnJarPath);
    }
  }

  private static void executeLocalEventServer(Class clazz, String alpnPath, String alpnJarPath) throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(clazz);
    final File directory = new File(directoryPath);
    final File lockfile = new File(directoryPath, "event-server");

    if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
      try {
        if (isHealthy()) {
          return;
        }
        logger.info("Execute the event-server from {}", directory);
        final Path jar = Paths.get(directory.getPath(), "72-event-server", "target", "event-server-capsule.jar");
        final Path config = Paths.get(directory.getPath(), "72-event-server", "event-service-config.yml");
        String alpn = System.getProperty("user.home") + "/.m2/repository/" + alpnJarPath;

        if (!new File(alpn).exists()) {
          // if maven repo is not in the home dir, this might be a jenkins job, check in the special location.
          alpn = alpnPath + alpnJarPath;
          if (!new File(alpn).exists()) {
            throw new FileNotFoundException("Missing alpn file");
          }
        }

        for (int i = 0; i < 10; i++) {
          logger.info("***");
        }

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xms1024m");

        addGCVMOptions(command);

        command.add("-Dfile.encoding=UTF-8");
        command.add("-Xbootclasspath/p:" + alpn);

        addJacocoAgentVM(jar, command);

        addJarConfig(jar, config, command);

        logger.info(Strings.join(command, " "));

        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.directory(directory);
        processExecutor.command(command);

        processExecutor.redirectOutput(Slf4jStream.of(logger).asInfo());
        processExecutor.redirectError(Slf4jStream.of(logger).asError());

        final StartedProcess startedProcess = processExecutor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(startedProcess.getProcess()::destroy));
        Poller.pollFor(ofMinutes(2), ofSeconds(2), EventServerExecutor::isHealthy);
      } catch (RuntimeException | IOException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  @SneakyThrows
  private Channel makeChannel() {
    SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    return NettyChannelBuilder.forTarget("localhost:9890").sslContext(sslContext).build();
  }
  private static Exception previous = new Exception();

  private static boolean isHealthy() {
    try {
      if (HealthGrpc.newBlockingStub(getChannel()).check(HealthCheckRequest.newBuilder().build()).getStatus()
          != ServingStatus.SERVING) {
        return false;
      }
    } catch (Exception exception) {
      if (exception.getMessage().equals(previous.getMessage())) {
        logger.info("not healthy");
      } else {
        logger.info("not healthy - {}", exception.getMessage());
        previous = exception;
      }
      return false;
    }
    logger.info("healthy");
    return true;
  }

  public static void main(String[] args) throws IOException {
    ensureEventServer(EventServerExecutor.class, alpn, alpnJar);
  }
}
