package software.wings.beans.command;

import software.wings.beans.Log.LogLevel;

public class NoopExecutionCallback implements LogCallback {
  @Override
  public void saveExecutionLog(String line) {}

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel) {}

  @Override
  public void saveExecutionLog(
      String line, LogLevel logLevel, CommandExecutionResult.CommandExecutionStatus commandExecutionStatus) {}
}
