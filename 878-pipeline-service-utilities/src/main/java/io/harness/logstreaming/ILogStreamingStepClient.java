/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logstreaming;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
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

  void closeAllOpenStreamsWithPrefix(String prefix);
}
