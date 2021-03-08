package io.harness.delegate.beans.logstreaming;

import io.harness.delegate.beans.logstreaming.CommandUnitProgress.CommandUnitProgressBuilder;
import io.harness.delegate.beans.taskprogress.ITaskProgressClient;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.logstreaming.LogLine;

import java.time.Instant;
import java.util.LinkedHashMap;

public class NGLogCallback implements LogCallback {
  private ILogStreamingTaskClient iLogStreamingTaskClient;
  private String commandUnitName;
  private CommandUnitsProgress commandUnitsProgress;

  public NGLogCallback(ILogStreamingTaskClient iLogStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    if (iLogStreamingTaskClient == null) {
      throw new InvalidRequestException(
          "Log Streaming Client is not present. Ensure that feature flag [LOG_STREAMING_INTEGRATION] is on.");
    }
    this.iLogStreamingTaskClient = iLogStreamingTaskClient;
    this.commandUnitName = commandUnitName;
    this.commandUnitsProgress = commandUnitsProgress;

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
    Instant now = Instant.now();
    LogLine logLine = LogLine.builder().message(line).level(logLevel).timestamp(now).build();
    iLogStreamingTaskClient.writeLogLine(logLine, commandUnitName);

    boolean terminalStatus = CommandExecutionStatus.isTerminalStatus(commandExecutionStatus);
    if (terminalStatus) {
      iLogStreamingTaskClient.closeStream(commandUnitName);
    }

    LinkedHashMap<String, CommandUnitProgress> commandUnitProgressMap =
        commandUnitsProgress.getCommandUnitProgressMap();

    CommandUnitProgressBuilder commandUnitProgressBuilder =
        CommandUnitProgress.builder().status(commandExecutionStatus);
    if (!commandUnitProgressMap.containsKey(commandUnitName)) {
      commandUnitProgressBuilder.startTime(now.toEpochMilli());
    } else {
      CommandUnitProgress commandUnitProgress = commandUnitProgressMap.get(commandUnitName);
      commandUnitProgressBuilder.startTime(commandUnitProgress.getStartTime());
    }
    if (terminalStatus) {
      commandUnitProgressBuilder.endTime(now.toEpochMilli());
    }
    commandUnitProgressMap.put(commandUnitName, commandUnitProgressBuilder.build());

    ITaskProgressClient taskProgressClient = iLogStreamingTaskClient.obtainTaskProgressClient();
    if (taskProgressClient != null) {
      taskProgressClient.sendTaskProgressUpdate(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress));
    }
  }
}