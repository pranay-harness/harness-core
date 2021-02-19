package io.harness.delegate.beans.logstreaming;

import io.harness.delegate.beans.taskprogress.ITaskProgressClient;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import java.time.Instant;

public class NGLogCallback implements LogCallback {
  private ILogStreamingTaskClient iLogStreamingTaskClient;
  private String commandUnitName;

  public NGLogCallback(
      ILogStreamingTaskClient iLogStreamingTaskClient, String commandUnitName, boolean shouldOpenStream) {
    if (iLogStreamingTaskClient == null) {
      throw new InvalidRequestException(
          "Log Streaming Client is not present. Ensure that feature flag [LOG_STREAMING_INTEGRATION] is on.");
    }
    this.iLogStreamingTaskClient = iLogStreamingTaskClient;
    this.commandUnitName = commandUnitName;

    if (shouldOpenStream) {
      iLogStreamingTaskClient.openStream(commandUnitName);
    }
  }

  @Override
  public void saveExecutionLog(String line) {
    saveExecutionLog(line, LogLevel.INFO);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel) {
    saveExecutionLog(line, logLevel, CommandExecutionStatus.RUNNING);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    LogLine logLine = LogLine.builder().message(line).level(logLevel).timestamp(Instant.now()).build();
    iLogStreamingTaskClient.writeLogLine(logLine, commandUnitName);

    if (CommandExecutionStatus.isTerminalStatus(commandExecutionStatus)) {
      iLogStreamingTaskClient.closeStream(commandUnitName);
    }

    ITaskProgressClient taskProgressClient = iLogStreamingTaskClient.obtainTaskProgressClient();
    if (taskProgressClient != null) {
      CommandUnitStatusProgress commandUnitStatusProgress = CommandUnitStatusProgress.builder()
              .commandUnitName(commandUnitName)
              .commandExecutionStatus(commandExecutionStatus)
              .build();
      taskProgressClient.sendTaskProgressUpdate(commandUnitStatusProgress);
    }
  }
}
