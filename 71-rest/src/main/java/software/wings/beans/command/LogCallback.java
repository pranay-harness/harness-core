package software.wings.beans.command;

import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

public interface LogCallback {
  void saveExecutionLog(String line);

  void saveExecutionLog(String line, LogLevel logLevel);

  void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus);
}
