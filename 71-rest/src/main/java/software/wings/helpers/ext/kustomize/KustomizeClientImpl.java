package software.wings.helpers.ext.kustomize;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_BINARY_PATH;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_BUILD_COMMAND;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_BUILD_COMMAND_WITH_PLUGINS;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_COMMAND_TIMEOUT;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_DIR_PATH;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.XDG_CONFIG_HOME;

import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

@Slf4j
@Singleton
public class KustomizeClientImpl implements KustomizeClient {
  @Override
  public CliResponse build(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeDirPath,
      @Nonnull String kustomizeBinaryPath, @Nonnull ExecutionLogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String kustomizeBuildCommand = KUSTOMIZE_BUILD_COMMAND.replace(KUSTOMIZE_BINARY_PATH, kustomizeBinaryPath)
                                       .replace(KUSTOMIZE_DIR_PATH, kustomizeDirPath);
    return executeCliCommand(kustomizeBuildCommand, KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(),
        manifestFilesDirectory, executionLogCallback);
  }

  @Override
  public CliResponse buildWithPlugins(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeDirPath,
      @Nonnull String kustomizeBinaryPath, @Nonnull String pluginPath,
      @Nonnull ExecutionLogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    String kustomizeBuildCommand =
        KUSTOMIZE_BUILD_COMMAND_WITH_PLUGINS.replace(KUSTOMIZE_BINARY_PATH, kustomizeBinaryPath)
            .replace(KUSTOMIZE_DIR_PATH, kustomizeDirPath)
            .replace(XDG_CONFIG_HOME, pluginPath);
    return executeCliCommand(kustomizeBuildCommand, KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(),
        manifestFilesDirectory, executionLogCallback);
  }

  CliResponse executeCliCommand(String command, long timeoutInMillis, Map<String, String> envVariables,
      String directory, ExecutionLogCallback executionLogCallback)
      throws IOException, InterruptedException, TimeoutException {
    executionLogCallback.saveExecutionLog(command, LogLevel.INFO, RUNNING);
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(timeoutInMillis, TimeUnit.MILLISECONDS)
                                          .command("/bin/sh", "-c", command)
                                          .readOutput(true)
                                          .environment(envVariables)
                                          .directory(new File(directory))
                                          .redirectOutput(new LogOutputStream() {
                                            @Override
                                            protected void processLine(String line) {
                                              logger.info(line);
                                            }
                                          });

    ProcessResult processResult = processExecutor.execute();
    CommandExecutionStatus status = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
    return CliResponse.builder().commandExecutionStatus(status).output(processResult.outputString()).build();
  }
}
