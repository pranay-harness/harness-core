/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.verification;

import software.wings.verification.CVActivityLog;
import software.wings.verification.CVActivityLog.LogLevel;

import java.util.List;

public interface CVActivityLogService {
  List<CVActivityLog> findByCVConfigId(String cvConfigId, long startTimeEpochMinute, long endTimeEpochMinute);
  Logger getLogger(String accountId, String cvConfigId, long dataCollectionMinute, String stateExecutionId);
  Logger getLoggerByCVConfigId(String accountId, String cvConfigId, long dataCollectionMinute);
  Logger getLoggerByStateExecutionId(String accountId, String stateExecutionId);
  List<CVActivityLog> findByStateExecutionId(String stateExecutionId);
  void saveActivityLogs(List<CVActivityLog> cvActivityLogs);
  List<CVActivityLog> getActivityLogs(
      String accountId, String stateExecutionId, String cvConfigId, long startTimeEpochMinute, long endTimeEpochMinute);

  interface Logger {
    default void info(String message, long... timestampParams) {
      this.appendLog(LogLevel.INFO, message, timestampParams);
    }
    default void warn(String message, long... timestampParams) {
      this.appendLog(LogLevel.WARN, message, timestampParams);
    }
    default void error(String message, long... timestampParams) {
      this.appendLog(LogLevel.ERROR, message, timestampParams);
    }

    /**
     * use %t in the log message with timestamp params to localize timestamp in the UI.
     * @param logLevel
     * @param message
     * @param timestampParams epoch timestamp in millis
     */
    void appendLog(LogLevel logLevel, String message, long... timestampParams);
  }
}
