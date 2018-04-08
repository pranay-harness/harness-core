package software.wings.beans.command;

import static software.wings.beans.Log.Builder.aLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;

/**
 * Created by anubhaw on 2/14/17.
 */
public class ExecutionLogCallback implements LogCallback {
  private transient DelegateLogService logService;
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private static final Logger logger = LoggerFactory.getLogger(ExecutionLogCallback.class);

  public ExecutionLogCallback() {
    // do nothing callback
  }

  public ExecutionLogCallback(
      DelegateLogService logService, String accountId, String appId, String activityId, String commandName) {
    this.logService = logService;
    this.accountId = accountId;
    this.appId = appId;
    this.activityId = activityId;
    this.commandName = commandName;
  }

  public void saveExecutionLog(String line) {
    saveExecutionLog(line, LogLevel.INFO);
  }

  public void saveExecutionLog(String line, LogLevel logLevel) {
    saveExecutionLog(line, logLevel, CommandExecutionStatus.RUNNING);
  }

  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    if (logService != null) {
      logService.save(accountId,
          aLog()
              .withAppId(appId)
              .withActivityId(activityId)
              .withLogLevel(logLevel)
              .withCommandUnitName(commandName)
              .withLogLine(line)
              .withExecutionResult(commandExecutionStatus)
              .build());
    } else {
      logger.error("No logService injected. Couldn't save log [{}:{}]", logLevel, line);
    }
  }

  public void setLogService(DelegateLogService logService) {
    this.logService = logService;
  }
}
