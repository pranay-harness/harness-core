package io.harness.logging;

public interface LogCallback {
  void saveExecutionLog(String line);

  void saveExecutionLog(String line, LogLevel logLevel);

  void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus);
}
