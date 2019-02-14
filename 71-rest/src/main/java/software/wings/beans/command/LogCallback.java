package software.wings.beans.command;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.Log.LogLevel;

public interface LogCallback {
  void saveExecutionLog(String line);

  void saveExecutionLog(String line, LogLevel logLevel);

  void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus);
}
