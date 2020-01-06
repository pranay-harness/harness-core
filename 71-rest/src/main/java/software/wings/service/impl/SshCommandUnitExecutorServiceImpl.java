package software.wings.service.impl;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Log.Builder;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ShellCommandExecutionContext;
import software.wings.beans.infrastructure.Host;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.core.ssh.executors.ScriptExecutor;
import software.wings.core.ssh.executors.ScriptSshExecutor;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.utils.SshHelperUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class SshCommandUnitExecutorServiceImpl.
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class SshCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  /**
   * The Log service.
   */
  @Inject private DelegateLogService logService;

  @Inject private TimeLimiter timeLimiter;

  @Inject private Injector injector;

  @Inject private SshExecutorFactory sshExecutorFactory;

  @Inject private ShellExecutorFactory shellExecutorFactory;

  @Override
  public void cleanup(String activityId, Host host) {
    ScriptSshExecutor.evictAndDisconnectCachedSession(activityId, host.getHostName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandExecutionStatus execute(CommandUnit commandUnit, CommandExecutionContext context) {
    String activityId = context.getActivityId();

    final Builder logBuilder =
        aLog().withAppId(context.getAppId()).withActivityId(activityId).withCommandUnitName(commandUnit.getName());

    logService.save(context.getAccountId(),
        logBuilder.withLogLevel(INFO)
            .withLogLine(format("Begin execution of command: %s", commandUnit.getName()))
            .withExecutionResult(RUNNING)
            .build());

    CommandExecutionStatus commandExecutionStatus = FAILURE;
    ScriptExecutor executor;
    if (context.isExecuteOnDelegate()) {
      ShellExecutorConfig shellExecutorConfig = ShellExecutorConfig.builder()
                                                    .accountId(context.getAccountId())
                                                    .appId(context.getAppId())
                                                    .executionId(context.getActivityId())
                                                    .commandUnitName(commandUnit.getName())
                                                    .environment(context.getEnvVariables())
                                                    .build();
      executor = shellExecutorFactory.getExecutor(shellExecutorConfig);
    } else {
      SshSessionConfig sshSessionConfig = SshHelperUtils.createSshSessionConfig(commandUnit.getName(), context);
      executor = sshExecutorFactory.getExecutor(sshSessionConfig);
    }

    ShellCommandExecutionContext shellCommandExecutionContext = new ShellCommandExecutionContext(context);
    shellCommandExecutionContext.setExecutor(executor);

    injector.injectMembers(commandUnit);

    try {
      long timeoutMs = context.getTimeout() == null ? TimeUnit.MINUTES.toMillis(10) : context.getTimeout().longValue();
      commandExecutionStatus = timeLimiter.callWithTimeout(
          () -> commandUnit.execute(shellCommandExecutionContext), timeoutMs, TimeUnit.MILLISECONDS, true);
    } catch (InterruptedException | TimeoutException | UncheckedTimeoutException e) {
      logService.save(context.getAccountId(),
          logBuilder.withLogLevel(ERROR)
              .withLogLine("Command execution timed out")
              .withExecutionResult(commandExecutionStatus)
              .build());
      throw new WingsException(ErrorCode.SOCKET_CONNECTION_TIMEOUT);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof WingsException) {
        WingsException ex = (WingsException) e.getCause();
        String errorMessage = ExceptionUtils.getMessage(ex);
        logService.save(context.getAccountId(),
            logBuilder.withLogLevel(ERROR)
                .withLogLine(errorMessage)
                .withExecutionResult(commandExecutionStatus)
                .build());
        throw(WingsException) e.getCause();
      } else {
        logService.save(context.getAccountId(),
            logBuilder.withLogLevel(ERROR)
                .withLogLine("Unknown Error " + e.getCause().getMessage())
                .withExecutionResult(commandExecutionStatus)
                .build());

        throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
      }
    } catch (WingsException e) {
      final List<ResponseMessage> messageList = ExceptionLogger.getResponseMessageList(e, REST_API);
      if (!messageList.isEmpty()) {
        if (messageList.get(0).getCode() == ErrorCode.INVALID_KEY
            || messageList.get(0).getCode() == ErrorCode.INVALID_CREDENTIAL) {
          logService.save(context.getAccountId(),
              logBuilder.withLogLevel(ERROR)
                  .withLogLine("Command execution failed: invalid key")
                  .withExecutionResult(commandExecutionStatus)
                  .build());
          throw e;
        }
        logService.save(context.getAccountId(),
            logBuilder.withLogLevel(ERROR)
                .withLogLine("Command execution failed: Reason:" + messageList.get(0).getMessage())
                .withExecutionResult(commandExecutionStatus)
                .build());
        throw e;
      } else {
        logService.save(context.getAccountId(),
            logBuilder.withLogLevel(ERROR)
                .withLogLine("Command execution failed. Reason:" + e.getMessage())
                .withExecutionResult(commandExecutionStatus)
                .build());
        throw e;
      }
    } catch (Exception e) {
      logService.save(context.getAccountId(),
          logBuilder.withLogLevel(ERROR)
              .withLogLine("Command execution failed. Reason: " + e.getMessage())
              .withExecutionResult(commandExecutionStatus)
              .build());
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }

    logService.save(context.getAccountId(),
        logBuilder.withLogLevel(SUCCESS == commandExecutionStatus ? INFO : ERROR)
            .withLogLine("Command execution finished with status " + commandExecutionStatus)
            .withExecutionResult(commandExecutionStatus)
            .build());

    commandUnit.setCommandExecutionStatus(commandExecutionStatus);
    return commandExecutionStatus;
  }
}
