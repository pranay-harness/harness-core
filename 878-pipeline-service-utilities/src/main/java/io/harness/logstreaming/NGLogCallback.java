/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logstreaming;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.time.Instant;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGLogCallback implements LogCallback {
  private final ILogStreamingStepClient logStreamingClient;
  private final String logSuffix;

  public NGLogCallback(LogStreamingStepClientFactory logStreamingStepClientFactory, Ambiance ambiance, String logSuffix,
      boolean shouldOpenStream) {
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
