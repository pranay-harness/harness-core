package software.wings.service.impl;

import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.common.Constants.WINDOWS_HOME_DIR;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.InitPowerShellCommandUnit;
import software.wings.beans.command.ShellCommandExecutionContext;
import software.wings.core.winrm.executors.WinRmExecutor;
import software.wings.core.winrm.executors.WinRmExecutorFactory;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.CommandUnitExecutorService;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class WinRMCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  @Inject private DelegateLogService logService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Injector injector;
  @Inject private WinRmExecutorFactory winRmExecutorFactory;

  @Override
  public CommandExecutionStatus execute(CommandUnit commandUnit, CommandExecutionContext context) {
    String activityId = context.getActivityId();
    String publicDns = context.getHost().getPublicDns();
    logService.save(context.getAccountId(),
        aLog()
            .appId(context.getAppId())
            .hostName(publicDns)
            .activityId(activityId)
            .logLevel(INFO)
            .commandUnitName(commandUnit.getName())
            .logLine(format("Begin execution of command: %s", commandUnit.getName()))
            .executionResult(RUNNING)
            .build());

    CommandExecutionStatus commandExecutionStatus = FAILURE;

    String commandPath =
        (commandUnit instanceof InitPowerShellCommandUnit) ? WINDOWS_HOME_DIR : context.getWindowsRuntimePath();

    WinRmSessionConfig winRmSessionConfig = context.winrmSessionConfig(commandUnit.getName(), commandPath);
    WinRmExecutor winRmExecutor =
        winRmExecutorFactory.getExecutor(winRmSessionConfig, context.isDisableWinRMCommandEncodingFFSet());

    ShellCommandExecutionContext shellCommandExecutionContext = new ShellCommandExecutionContext(context);
    shellCommandExecutionContext.setExecutor(winRmExecutor);

    injector.injectMembers(commandUnit);

    try {
      long timeoutMs = context.getTimeout() == null ? TimeUnit.MINUTES.toMillis(10) : context.getTimeout().longValue();
      commandExecutionStatus = timeLimiter.callWithTimeout(
          () -> commandUnit.execute(shellCommandExecutionContext), timeoutMs, TimeUnit.MILLISECONDS, true);
    } catch (InterruptedException | TimeoutException | UncheckedTimeoutException e) {
      logService.save(context.getAccountId(),
          aLog()
              .appId(context.getAppId())
              .activityId(activityId)
              .hostName(publicDns)
              .logLevel(SUCCESS == commandExecutionStatus ? INFO : ERROR)
              .logLine("Command execution timed out")
              .commandUnitName(commandUnit.getName())
              .executionResult(commandExecutionStatus)
              .build());
      throw new WingsException(ErrorCode.SOCKET_CONNECTION_TIMEOUT);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof WingsException) {
        WingsException ex = (WingsException) e.getCause();
        String errorMessage = ExceptionUtils.getMessage(ex);
        logService.save(context.getAccountId(),
            aLog()
                .appId(context.getAppId())
                .activityId(activityId)
                .hostName(publicDns)
                .commandUnitName(commandUnit.getName())
                .logLevel(SUCCESS == commandExecutionStatus ? INFO : ERROR)
                .logLine(errorMessage)
                .executionResult(commandExecutionStatus)
                .build());
        throw(WingsException) e.getCause();
      } else {
        logService.save(context.getAccountId(),
            aLog()
                .appId(context.getAppId())
                .activityId(activityId)
                .hostName(publicDns)
                .logLevel(SUCCESS == commandExecutionStatus ? INFO : ERROR)
                .logLine("Unknown Error " + e.getCause().getMessage())
                .commandUnitName(commandUnit.getName())
                .executionResult(commandExecutionStatus)
                .build());

        throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
      }
    } catch (WingsException exception) {
      final List<ResponseMessage> messageList = ExceptionLogger.getResponseMessageList(exception, REST_API);
      if (!messageList.isEmpty()) {
        if (messageList.get(0).getCode() == ErrorCode.INVALID_KEY
            || messageList.get(0).getCode() == ErrorCode.INVALID_CREDENTIAL) {
          logService.save(context.getAccountId(),
              aLog()
                  .appId(context.getAppId())
                  .activityId(activityId)
                  .hostName(publicDns)
                  .logLevel(SUCCESS == commandExecutionStatus ? INFO : ERROR)
                  .logLine("Command execution failed: invalid key")
                  .commandUnitName(commandUnit.getName())
                  .executionResult(commandExecutionStatus)
                  .build());
          throw exception;
        }
      } else {
        logger.error("Error while executing command", exception);
        logService.save(context.getAccountId(),
            aLog()
                .appId(context.getAppId())
                .activityId(activityId)
                .hostName(publicDns)
                .logLevel(SUCCESS == commandExecutionStatus ? INFO : ERROR)
                .logLine("Command execution failed")
                .commandUnitName(commandUnit.getName())
                .executionResult(commandExecutionStatus)
                .build());
        throw new WingsException(ErrorCode.UNKNOWN_ERROR, exception);
      }
    } catch (Exception e) {
      logService.save(context.getAccountId(),
          aLog()
              .appId(context.getAppId())
              .activityId(activityId)
              .hostName(publicDns)
              .logLevel(SUCCESS == commandExecutionStatus ? INFO : ERROR)
              .logLine("Command execution failed")
              .commandUnitName(commandUnit.getName())
              .executionResult(commandExecutionStatus)
              .build());
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }

    logService.save(context.getAccountId(),
        aLog()
            .appId(context.getAppId())
            .activityId(activityId)
            .hostName(publicDns)
            .logLevel(SUCCESS == commandExecutionStatus ? INFO : ERROR)
            .logLine("Command execution finished with status " + commandExecutionStatus)
            .commandUnitName(commandUnit.getName())
            .executionResult(commandExecutionStatus)
            .build());

    commandUnit.setCommandExecutionStatus(commandExecutionStatus);
    return commandExecutionStatus;
  }
}
