package software.wings.helpers.ext.cli;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;

import software.wings.beans.command.ExecutionLogCallback;

import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@Slf4j
@Singleton
public class CliHelper {
  @Nonnull
  public CliResponse executeCliCommand(String command, long timeoutInMillis, Map<String, String> envVariables,
      String directory, ExecutionLogCallback executionLogCallback)
      throws IOException, InterruptedException, TimeoutException {
    executionLogCallback.saveExecutionLog(command, LogLevel.INFO, RUNNING);
    ProcessExecutor processExecutor =
        new ProcessExecutor()
            .timeout(timeoutInMillis, TimeUnit.MILLISECONDS)
            .command("/bin/sh", "-c", command)
            .readOutput(true)
            .environment(envVariables)
            .directory(new File(directory))
            .redirectOutput(new LogOutputStream() {
              @Override
              protected void processLine(String line) {
                // Not logging as secrets will be exposed
              }
            })
            .redirectError(new LogOutputStream() {
              @Override
              protected void processLine(String line) {
                log.error(line);
                executionLogCallback.saveExecutionLog(line, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
              }
            });

    ProcessResult processResult = processExecutor.execute();
    CommandExecutionStatus status = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
    return CliResponse.builder().commandExecutionStatus(status).output(processResult.outputUTF8()).build();
  }
}
