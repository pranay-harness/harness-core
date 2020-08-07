package software.wings.service.impl;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.logging.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.CommandUnitExecutorService;

import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 6/23/17.
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class CodeDeployCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  /**
   * The Log service.
   */
  @Inject private DelegateLogService logService;

  @Inject private TimeLimiter timeLimiter;

  @Inject private Injector injector;

  @Override
  public CommandExecutionStatus execute(CommandUnit commandUnit, CommandExecutionContext context) {
    String activityId = context.getActivityId();
    logService.save(context.getAccountId(),
        aLog()
            .appId(context.getAppId())
            .activityId(activityId)
            .logLevel(INFO)
            .commandUnitName(commandUnit.getName())
            .logLine(format("Begin execution of command: %s", commandUnit.getName()))
            .executionResult(RUNNING)
            .build());

    CommandExecutionStatus commandExecutionStatus = FAILURE;
    injector.injectMembers(commandUnit);

    try {
      commandExecutionStatus = commandUnit.execute(context);
    } catch (Exception ex) {
      logger.error("Error while executing command", ex);
      logService.save(context.getAccountId(),
          aLog()
              .appId(context.getAppId())
              .activityId(activityId)
              .logLevel(ERROR)
              .logLine("Command execution failed")
              .commandUnitName(commandUnit.getName())
              .executionResult(commandExecutionStatus)
              .executionResult(FAILURE)
              .build());
    }

    logService.save(context.getAccountId(),
        aLog()
            .appId(context.getAppId())
            .activityId(activityId)
            .logLevel(INFO)
            .logLine("Command execution finished with status " + commandExecutionStatus)
            .commandUnitName(commandUnit.getName())
            .executionResult(commandExecutionStatus)
            .build());

    commandUnit.setCommandExecutionStatus(commandExecutionStatus);
    return commandExecutionStatus;
  }
}
