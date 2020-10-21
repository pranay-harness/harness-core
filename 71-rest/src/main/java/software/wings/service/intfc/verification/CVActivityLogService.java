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
    default void info(String log, long... timestampParams) {
      this.appendLog(LogLevel.INFO, log, timestampParams);
    }
    default void warn(String log, long... timestampParams) {
      this.appendLog(LogLevel.WARN, log, timestampParams);
    }
    default void error(String error, long... timestampParams) {
      this.appendLog(LogLevel.ERROR, error, timestampParams);
    }

    /**
     * use %t in the log message with timestamp params to localize timestamp in the UI.
     * @param logLevel
     * @param log
     * @param timestampParams epoch timestamp in millis
     */
    void appendLog(LogLevel logLevel, String log, long... timestampParams);
  }
}
