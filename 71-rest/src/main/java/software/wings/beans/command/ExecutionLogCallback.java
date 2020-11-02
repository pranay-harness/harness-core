package software.wings.beans.command;

import static software.wings.beans.Log.Builder.aLog;

import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.DelegateLogService;

/**
 * Created by anubhaw on 2/14/17.
 */
@Slf4j
public class ExecutionLogCallback implements LogCallback {
  private transient DelegateLogService logService;
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;

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

  @Override
  public void saveExecutionLog(String line) {
    saveExecutionLog(line, LogLevel.INFO);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel) {
    saveExecutionLog(line, logLevel, CommandExecutionStatus.RUNNING);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    if (logService != null) {
      logService.save(accountId,
          aLog()
              .appId(appId)
              .activityId(activityId)
              .logLevel(logLevel)
              .commandUnitName(commandName)
              .logLine(line)
              .executionResult(commandExecutionStatus)
              .build());
    } else {
      log.error("No logService injected. Couldn't save log [{}:{}]", logLevel, line);
    }
  }

  public void setLogService(DelegateLogService logService) {
    this.logService = logService;
  }
}
