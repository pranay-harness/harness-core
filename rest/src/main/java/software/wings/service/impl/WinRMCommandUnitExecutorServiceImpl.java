package software.wings.service.impl;

import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;

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
import software.wings.core.winrm.executors.WinRmExecutorFactory;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.CommandUnitExecutorService;

import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
public class WinRMCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  private static final Logger logger = LoggerFactory.getLogger(WinRMCommandUnitExecutorServiceImpl.class);

  @Inject private DelegateLogService logService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Injector injector;
  @Inject private WinRmExecutorFactory winRmExecutorFactory;

  @Override
  public void cleanup(String activityId, Host host) {}

  @Override
  public CommandExecutionStatus execute(Host host, CommandUnit commandUnit, CommandExecutionContext context) {
    String activityId = context.getActivityId();
    logService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withHostName(host.getPublicDns())
            .withActivityId(activityId)
            .withLogLevel(INFO)
            .withCommandUnitName(commandUnit.getName())
            .withLogLine(format("Begin execution of command: %s", commandUnit.getName()))
            .withExecutionResult(RUNNING)
            .build());

    // ToDo Implementation

    return SUCCESS;
  }
}
