package software.wings.service.impl;

import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.infrastructure.Host;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.CommandUnitExecutorService;

import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 6/23/17.
 */
@ValidateOnExecution
@Singleton
public class CodeDeployCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  private static final Logger logger = LoggerFactory.getLogger(CodeDeployCommandUnitExecutorServiceImpl.class);
  /**
   * The Log service.
   */
  @Inject private DelegateLogService logService;

  @Inject private TimeLimiter timeLimiter;

  @Inject private Injector injector;

  @Override
  public void cleanup(String activityId, Host host) {
    // TODO::
  }

  @Override
  public CommandExecutionStatus execute(Host host, CommandUnit commandUnit, CommandExecutionContext context) {
    String activityId = context.getActivityId();
    logService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withActivityId(activityId)
            .withLogLevel(INFO)
            .withCommandUnitName(commandUnit.getName())
            .withLogLine(format("Begin execution of command: %s", commandUnit.getName()))
            .withExecutionResult(RUNNING)
            .build());

    CommandExecutionStatus commandExecutionStatus = FAILURE;
    injector.injectMembers(commandUnit);

    try {
      commandExecutionStatus = commandUnit.execute(context);
    } catch (Exception ex) {
      logger.error("Error while executing command", ex);
      logService.save(context.getAccountId(),
          aLog()
              .withAppId(context.getAppId())
              .withActivityId(activityId)
              .withLogLevel(ERROR)
              .withLogLine("Command execution failed")
              .withCommandUnitName(commandUnit.getName())
              .withExecutionResult(commandExecutionStatus)
              .withExecutionResult(FAILURE)
              .build());
    }

    logService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withActivityId(activityId)
            .withLogLevel(INFO)
            .withLogLine("Command execution finished with status " + commandExecutionStatus)
            .withCommandUnitName(commandUnit.getName())
            .withExecutionResult(commandExecutionStatus)
            .build());

    commandUnit.setCommandExecutionStatus(commandExecutionStatus);
    return commandExecutionStatus;
  }
}
