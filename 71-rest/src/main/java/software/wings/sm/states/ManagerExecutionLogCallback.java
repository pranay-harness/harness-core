package software.wings.sm.states;

import com.google.common.base.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.beans.Log.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.LogCallback;
import software.wings.service.intfc.LogService;

public class ManagerExecutionLogCallback implements LogCallback {
  private transient LogService logService;
  private Builder logBuilder;
  private String activityId;
  private static final Logger logger = LoggerFactory.getLogger(ManagerExecutionLogCallback.class);

  public ManagerExecutionLogCallback() {}

  public ManagerExecutionLogCallback(LogService logService, Builder logBuilder, String activityId) {
    this.logService = logService;
    this.logBuilder = logBuilder;
    this.activityId = activityId;
  }

  public void saveExecutionLog(String line) {
    saveExecutionLog(line, CommandExecutionStatus.RUNNING, LogLevel.INFO);
  }

  public void saveExecutionLog(String line, CommandExecutionStatus commandExecutionStatus) {
    saveExecutionLog(line, commandExecutionStatus, LogLevel.INFO);
  }

  public void saveExecutionLog(String line, LogLevel logLevel) {
    saveExecutionLog(line, CommandExecutionStatus.RUNNING, logLevel);
  }

  private void saveExecutionLog(String line, CommandExecutionStatus status, LogLevel logLevel) {
    saveExecutionLog(line, logLevel, status);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    if (logService != null) {
      Log log =
          logBuilder.but().withLogLevel(logLevel).withExecutionResult(commandExecutionStatus).withLogLine(line).build();
      logService.batchedSaveCommandUnitLogs(activityId, log.getCommandUnitName(), log);
    } else {
      logger.warn("No logService injected. Couldn't save log [{}]", line);
    }
  }

  public LogService getLogService() {
    return logService;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagerExecutionLogCallback that = (ManagerExecutionLogCallback) o;
    return Objects.equal(activityId, that.activityId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(activityId);
  }
}
