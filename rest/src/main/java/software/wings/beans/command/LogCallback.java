package software.wings.beans.command;

import software.wings.beans.Log.LogLevel;

public interface LogCallback {
  void saveExecutionLog(String line, LogLevel logLevel);

  void saveExecutionLog(String line);
}
