package software.wings.service.impl;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.AbstractCommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.command.AbstractCommandUnit.ExecutionResult.SUCCESS;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCodes;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.SshCommandExecutionContext;
import software.wings.beans.infrastructure.Host;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.core.ssh.executors.AbstractSshExecutor;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.utils.SshHelperUtil;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class SshCommandUnitExecutorServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class SshCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  /**
   * The Log service.
   */
  @Inject private DelegateLogService logService;

  @Inject private TimeLimiter timeLimiter;

  @Inject private Injector injector;

  @Inject private SshExecutorFactory sshExecutorFactory;

  @Override
  public void cleanup(String activityId, Host host) {
    AbstractSshExecutor.evictAndDisconnectCachedSession(activityId, host.getHostName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResult execute(Host host, CommandUnit commandUnit, CommandExecutionContext context) {
    String activityId = context.getActivityId();
    logService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withHostName(host.getHostName())
            .withActivityId(activityId)
            .withLogLevel(INFO)
            .withCommandUnitName(commandUnit.getName())
            .withLogLine(format("Begin execution of command: %s", commandUnit.getName()))
            .build());

    ExecutionResult executionResult = FAILURE;

    SshSessionConfig sshSessionConfig =
        SshHelperUtil.getSshSessionConfig(host.getHostName(), commandUnit.getName(), context);
    SshExecutor executor = sshExecutorFactory.getExecutor(sshSessionConfig.getExecutorType()); // TODO: Reuse executor
    executor.init(sshSessionConfig);

    SshCommandExecutionContext sshCommandExecutionContext = new SshCommandExecutionContext(context);
    sshCommandExecutionContext.setSshExecutor(executor);

    injector.injectMembers(commandUnit);

    try {
      executionResult = timeLimiter.callWithTimeout(()
                                                        -> commandUnit.execute(sshCommandExecutionContext),
          commandUnit.getCommandExecutionTimeout(), TimeUnit.MILLISECONDS, true);
    } catch (InterruptedException | TimeoutException | UncheckedTimeoutException e) {
      logService.save(context.getAccountId(),
          aLog()
              .withAppId(context.getAppId())
              .withActivityId(activityId)
              .withHostName(host.getHostName())
              .withLogLevel(SUCCESS.equals(executionResult) ? INFO : ERROR)
              .withLogLine("Command execution timed out")
              .withCommandUnitName(commandUnit.getName())
              .withExecutionResult(executionResult)
              .build());
      throw new WingsException(ErrorCodes.SOCKET_CONNECTION_TIMEOUT);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof WingsException) {
        WingsException ex = (WingsException) e.getCause();
        String errorMessage =
            Joiner.on(",").join(ex.getResponseMessageList()
                                    .stream()
                                    .map(responseMessage
                                        -> ResponseCodeCache.getInstance()
                                               .getResponseMessage(responseMessage.getCode(), ex.getParams())
                                               .getMessage())
                                    .collect(toList()));
        logService.save(context.getAccountId(),
            aLog()
                .withAppId(context.getAppId())
                .withActivityId(activityId)
                .withHostName(host.getHostName())
                .withCommandUnitName(commandUnit.getName())
                .withLogLevel(SUCCESS.equals(executionResult) ? INFO : ERROR)
                .withLogLine(errorMessage)
                .withExecutionResult(executionResult)
                .build());
        throw(WingsException) e.getCause();
      } else {
        logService.save(context.getAccountId(),
            aLog()
                .withAppId(context.getAppId())
                .withActivityId(activityId)
                .withHostName(host.getHostName())
                .withLogLevel(SUCCESS.equals(executionResult) ? INFO : ERROR)
                .withLogLine("Unknown Error " + e.getCause().getMessage())
                .withCommandUnitName(commandUnit.getName())
                .withExecutionResult(executionResult)
                .build());

        throw new WingsException(ErrorCodes.UNKNOWN_ERROR, "", e);
      }
    } catch (Exception e) {
      logger.error("Error while executing command ", e);
      logService.save(context.getAccountId(),
          aLog()
              .withAppId(context.getAppId())
              .withActivityId(activityId)
              .withHostName(host.getHostName())
              .withLogLevel(SUCCESS.equals(executionResult) ? INFO : ERROR)
              .withLogLine("Command execution failed")
              .withCommandUnitName(commandUnit.getName())
              .withExecutionResult(executionResult)
              .build());
      throw new WingsException(ErrorCodes.UNKNOWN_ERROR);
    }

    logService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withActivityId(activityId)
            .withHostName(host.getHostName())
            .withLogLevel(SUCCESS.equals(executionResult) ? INFO : ERROR)
            .withLogLine("Command execution finished with status " + executionResult)
            .withCommandUnitName(commandUnit.getName())
            .withExecutionResult(executionResult)
            .build());

    commandUnit.setExecutionResult(executionResult);
    return executionResult;
  }
}
