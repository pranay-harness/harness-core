package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

public class NoopExecutionCallback implements LogCallback {
  @Override
  public void saveExecutionLog(String line) {}

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel) {}

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    // Nothing required
  }
}
