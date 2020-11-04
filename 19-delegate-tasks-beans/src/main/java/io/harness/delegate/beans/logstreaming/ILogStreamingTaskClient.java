package io.harness.delegate.beans.logstreaming;

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
}
