package io.harness.pms.sdk.core.execution.invokers;
import io.harness.logStreaming.ILogStreamingStepClient;
import io.harness.logStreaming.LogStreamingStepClientFactory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.logstreaming.LogLine;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.time.Instant;

public class NGManagerLogCallback implements LogCallback {
  ILogStreamingStepClient logStreamingClient;
  String logSuffix;

  public NGManagerLogCallback(LogStreamingStepClientFactory logStreamingStepClientFactory, Ambiance ambiance,
      String logSuffix, boolean shouldOpenStream) {
    this.logStreamingClient = logStreamingStepClientFactory.getLogStreamingStepClient(ambiance);
    this.logSuffix = logSuffix;
    if (shouldOpenStream) {
      logStreamingClient.openStream(logSuffix);
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
    logStreamingClient.writeLogLine(logLine, logSuffix);
    boolean terminalStatus = CommandExecutionStatus.isTerminalStatus(commandExecutionStatus);
    if (terminalStatus) {
      logStreamingClient.closeStream(logSuffix);
    }
  }
}
