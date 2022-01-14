package io.harness.cli;

import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import org.zeroturnaround.exec.stream.LogOutputStream;

public class LogCallbackOutputStream extends LogOutputStream {
  private LogCallback logCallback;

  public LogCallbackOutputStream(LogCallback logCallback) {
    this.logCallback = logCallback;
  }

  @Override
  protected void processLine(String line) {
    logCallback.saveExecutionLog(line, LogLevel.INFO, CommandExecutionStatus.RUNNING);
  }
}
