package io.harness.testframework.framework;

import static io.harness.testframework.framework.utils.ExecutorUtils.addGCVMOptions;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJacocoAgentVM;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJarConfig;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.fabric8.utils.Strings;
import io.harness.filesystem.FileIo;
import io.harness.resource.Project;
import io.harness.threading.Poller;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
@Slf4j
public class WatcherExecutor {
  private static boolean failedAlready;
  private static volatile Process watcherProcess;

  public static void ensureWatcher(Class clazz) throws IOException {
    if (!isHealthy()) {
      executeLocalWatcher(clazz);
    }
  }

  private static void executeLocalWatcher(Class clazz) throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(clazz);
    final File directory = new File(directoryPath);
    final File lockfile = new File(directoryPath, "watcher");

    if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
      try {
        if (isHealthy()) {
          return;
        }
        logger.info("Execute the watcher from {}", directory);
        final Path jar = Paths.get(directory.getPath(), "82-watcher", "target", "watcher-capsule.jar");
        final Path config = Paths.get(directory.getPath(), "82-watcher", "config-watcher.yml");

        List<String> command = new ArrayList<>();
        command.add("java");

        addGCVMOptions(command);
        addJacocoAgentVM(jar, command);

        addJarConfig(jar, config, command);

        logger.info(Strings.join(command, " "));

        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.directory(directory);
        processExecutor.command(command);

        processExecutor.redirectOutput(Slf4jStream.of(logger).asInfo());
        processExecutor.redirectError(Slf4jStream.of(logger).asError());

        final StartedProcess startedProcess = processExecutor.start();
        watcherProcess = startedProcess.getProcess();
        Runtime.getRuntime().addShutdownHook(new Thread(watcherProcess::destroy));

        Poller.pollFor(ofMinutes(2), ofSeconds(2), WatcherExecutor::isHealthy);

      } catch (RuntimeException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  private static boolean isHealthy() {
    return watcherProcess != null && watcherProcess.isAlive();
  }

  public static void main(String[] args) throws IOException {
    ensureWatcher(WatcherExecutor.class);
  }
}
