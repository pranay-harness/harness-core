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
public class ExecutionLogCallback {
  private transient DelegateLogService logService;
  private CommandExecutionContext commandExecutionContext;
  private String commandName;
  private static final Logger logger = LoggerFactory.getLogger(ExecutionLogCallback.class);

  public ExecutionLogCallback() {}

  public ExecutionLogCallback(CommandExecutionContext commandExecutionContext, String commandName) {
    this.commandExecutionContext = commandExecutionContext;
    this.commandName = commandName;
  }

  public void saveExecutionLog(String line, LogLevel logLevel) {
    if (logService != null) {
      logService.save(commandExecutionContext.getAccountId(),
          aLog()
              .withAppId(commandExecutionContext.getAppId())
              .withActivityId(commandExecutionContext.getActivityId())
              .withLogLevel(logLevel)
              .withCommandUnitName(commandName)
              .withLogLine(line)
              .withExecutionResult(CommandExecutionStatus.RUNNING)
              .build());
    } else {
      logger.error("No logService injected. Couldn't save log [{}:{}]", logLevel, line);
    }
  }

  public void setLogService(DelegateLogService logService) {
    this.logService = logService;
  }
}
