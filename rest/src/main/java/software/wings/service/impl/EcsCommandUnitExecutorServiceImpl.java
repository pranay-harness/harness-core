package software.wings.service.impl;

import static software.wings.beans.command.AbstractCommandUnit.ExecutionResult.FAILURE;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.infrastructure.Host;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.CommandUnitExecutorService;

import javax.validation.executable.ValidateOnExecution;

/**
 * The Class SshCommandUnitExecutorServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class EcsCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResult execute(Host host, CommandUnit commandUnit, CommandExecutionContext context) {
    String activityId = context.getActivityId();
    //    logService.save(context.getAccountId(),
    //    aLog().withAppId(context.getAppId()).withHostName(host.getHostName()).withActivityId(activityId).withLogLevel(INFO)
    //        .withCommandUnitName(commandUnit.getName()).withLogLine(format("Begin execution of command: %s",
    //        commandUnit.getName())).build());

    ExecutionResult executionResult = FAILURE;
    injector.injectMembers(commandUnit);

    try {
      executionResult = commandUnit.execute(context);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    commandUnit.setExecutionResult(executionResult);
    return executionResult;
  }
}
