package io.harness.delegate.beans.logstreaming;

import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import java.io.OutputStream;

public interface ILogStreamingTaskClient {
  /**
   * Open new log stream on log streaming service.
   */
  void openStream(String baseLogKeySuffix);

  /**
   * Close existing log stream on log streaming service
   */
  void closeStream(String baseLogKeySuffix);

  /**
   * Push log message to the existing log stream
   */
  void writeLogLine(LogLine logLine, String baseLogKeySuffix);

  /**
   * Provides an output stream for writing log messages (e.g. to be given to the ProcessExecutor)
   */
  OutputStream obtainLogOutputStream(LogLevel logLevel, String baseLogKeySuffix);

  /**
   * Provides an instance of LogCallback interface implementation for backward compatibility reasons, to be used in
   * delegate task implementations, instead of direct constructor invocation. LogService and accountId are provided by
   * the delegate, while appId and activityId should be available as part of task parameters. Please make sure that your
   * task parameters class implements {@link io.harness.delegate.task.Cd1ApplicationAccess} and {@link
   * io.harness.delegate.task.ActivityAccess} interfaces.
   */
  LogCallback obtainLogCallback(String commandName);
}
