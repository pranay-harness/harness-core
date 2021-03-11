package io.harness.logStreaming;

import io.harness.logstreaming.LogLine;

public interface ILogStreamingStepClient {
  /**
   * Open new log stream on log streaming service.
   */
  void openStream(String logKeySuffix);

  /**
   * Close existing log stream on log streaming service
   */
  void closeStream(String logKeySuffix);

  /**
   * Push log message to the existing log stream
   */
  void writeLogLine(LogLine logLine, String logKeySuffix);
}
