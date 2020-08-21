package io.harness.testframework.framework;

import static io.harness.testframework.framework.utils.ExecutorUtils.addGCVMOptions;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJacocoAgentVM;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJar;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Singleton;

import io.fabric8.utils.Strings;
import io.harness.filesystem.FileIo;
import io.harness.resource.Project;
import io.harness.threading.Poller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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

@Singleton
@Slf4j
public class BatchProcessingExecutor {
  private boolean failedAlready;
  private Path livenessMarker;

  public void ensureBatchProcessing(Class<?> clazz, String alpnPath, String alpnJarPath) throws IOException {
    if (!isHealthy()) {
      executeLocalBatchProcessing(clazz, alpnPath, alpnJarPath);
    }
  }

  private void executeLocalBatchProcessing(Class<?> clazz, String alpnPath, String alpnJarPath) throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(clazz);
    final File directory = new File(directoryPath);
    final File lockfile = new File(directoryPath, "batch-processing");

    if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
      try {
        if (isHealthy()) {
          return;
        }
        logger.info("Execute the batch-processing from {}", directory);
        Path jar = Paths.get(directory.getPath(), "78-batch-processing", "target", "batch-processing-capsule.jar");
        Path config = Paths.get(directory.getPath(), "78-batch-processing", "batch-processing-config.yml");
        String alpn = System.getProperty("user.home") + "/.m2/repository/" + alpnJarPath;

        if (!new File(alpn).exists()) {
          // if maven repo is not in the home dir, this might be a jenkins job, check in the special location.
          alpn = alpnPath + alpnJarPath;
          if (!new File(alpn).exists()) {
            throw new FileNotFoundException("Missing alpn file");
          }
        }

        livenessMarker = Paths.get(directory.getPath(), "batch-processing-up");
        // ensure liveness file is deleted.
        FileUtils.deleteQuietly(livenessMarker.toFile());

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

        addJar(jar, command);
        command.add("--config-file=" + config.toString());
        command.add("--ensure-timescale=false");

        logger.info(Strings.join(command, " "));

        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.directory(directory);
        processExecutor.command(command);

        processExecutor.redirectOutput(Slf4jStream.of(logger).asInfo());
        processExecutor.redirectError(Slf4jStream.of(logger).asError());

        final StartedProcess startedProcess = processExecutor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(startedProcess.getProcess()::destroy));
        Poller.pollFor(ofMinutes(5), ofSeconds(2), this ::isHealthy);
      } catch (RuntimeException | IOException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  private boolean isHealthy() {
    if (livenessMarker == null) {
      return false;
    }
    File livenessFile = livenessMarker.toFile();
    logger.info("Checking for liveness marker {}", livenessFile.getAbsolutePath());
    return livenessFile.exists();
  }

  public static void main(String[] args) throws IOException {
    final String alpnJar = "org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar";
    String alpn = "/home/jenkins/maven-repositories/0/";
    new BatchProcessingExecutor().ensureBatchProcessing(BatchProcessingExecutor.class, alpn, alpnJar);
  }
}
