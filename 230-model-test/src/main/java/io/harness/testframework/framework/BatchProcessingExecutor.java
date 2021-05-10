package io.harness.testframework.framework;

import static io.harness.testframework.framework.utils.ExecutorUtils.addGCVMOptions;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJacocoAgentVM;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJar;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.filesystem.FileIo;
import io.harness.project.Alpn;
import io.harness.resource.Project;
import io.harness.threading.Poller;

import com.google.inject.Singleton;
import io.fabric8.utils.Strings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

@Singleton
@Slf4j
public class BatchProcessingExecutor {
  private boolean failedAlready;
  private Path livenessMarker;

  public void ensureBatchProcessing(Class<?> clazz) throws IOException {
    if (!isHealthy()) {
      executeLocalBatchProcessing(clazz);
    }
  }

  private void executeLocalBatchProcessing(Class<?> clazz) throws IOException {
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
        log.info("Execute the batch-processing from {}", directory);
        Path jar = Paths.get(directory.getPath(), "280-batch-processing", "target", "batch-processing-capsule.jar");
        Path config = Paths.get(directory.getPath(), "280-batch-processing", "batch-processing-config.yml");
        String alpn = Alpn.location();

        createConfigFile(directory.getPath());

        livenessMarker = Paths.get(directory.getPath(), "batch-processing-up");
        // ensure liveness file is deleted.
        FileUtils.deleteQuietly(livenessMarker.toFile());

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
        command.add("--config-file=" + config.toString());
        command.add("--ensure-timescale=false");

        log.info(Strings.join(command, " "));

        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.directory(directory);
        processExecutor.command(command);

        processExecutor.redirectOutput(Slf4jStream.of(log).asInfo());
        processExecutor.redirectError(Slf4jStream.of(log).asError());

        final StartedProcess startedProcess = processExecutor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(startedProcess.getProcess()::destroy));
        Poller.pollFor(ofMinutes(5), ofSeconds(2), this::isHealthy);
      } catch (RuntimeException | IOException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        deleteConfigFile(directory.getPath());
        FileIo.releaseLock(lockfile);
      }
    }
  }

  private boolean isHealthy() {
    if (livenessMarker == null) {
      return false;
    }
    File livenessFile = livenessMarker.toFile();
    log.info("Checking for liveness marker {}", livenessFile.getAbsolutePath());
    return livenessFile.exists();
  }

  private void createConfigFile(String directory) {
    try {
      File config = new File(directory + "/batch-processing-config.yml");
      if (!config.exists() && config.createNewFile()) {
        FileWriter writer = new FileWriter(directory + "/batch-processing-config.yml");
        writer.write(
            "scheduler-jobs-config:\n  budgetAlertsJobCron: \"0 30 14 * * ?\"\n  weeklyReportsJobCron: \"0 0 14 * * MON\"");
        writer.close();
      }
    } catch (IOException e) {
      log.info("Error while creating config file for batch processing functional test");
    }
  }

  private void deleteConfigFile(String directory) {
    File config = new File(directory + "/batch-processing-config.yml");
    if (config.exists()) {
      config.delete();
    }
  }

  public static void main(String[] args) throws IOException {
    new BatchProcessingExecutor().ensureBatchProcessing(BatchProcessingExecutor.class);
  }
}
