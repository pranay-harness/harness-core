package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.common.Constants.WINDOWS_DEFAULT_COMMAND_PATH;
import static software.wings.exception.WingsException.ReportTarget.REST_API;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ShellCommandExecutionContext;
import software.wings.beans.infrastructure.Host;
import software.wings.core.winrm.executors.WinRmExecutor;
import software.wings.core.winrm.executors.WinRmExecutorFactory;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.utils.Misc;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    CommandExecutionStatus commandExecutionStatus = FAILURE;

    String commandPath =
        (commandUnit instanceof ExecCommandUnit) ? ((ExecCommandUnit) commandUnit).getCommandPath() : "";

    if (isEmpty(commandPath)) {
      commandPath = WINDOWS_DEFAULT_COMMAND_PATH;
    }

    WinRmSessionConfig winRmSessionConfig = context.winrmSessionConfig(commandUnit.getName(), commandPath);
    WinRmExecutor winRmExecutor = winRmExecutorFactory.getExecutor(winRmSessionConfig);

    ShellCommandExecutionContext shellCommandExecutionContext = new ShellCommandExecutionContext(context);
    shellCommandExecutionContext.setExecutor(winRmExecutor);

    injector.injectMembers(commandUnit);

    try {
      long timeoutMs = context.getTimeout() == null ? TimeUnit.MINUTES.toMillis(10) : context.getTimeout().longValue();
      commandExecutionStatus = timeLimiter.callWithTimeout(
          () -> commandUnit.execute(shellCommandExecutionContext), timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | TimeoutException | UncheckedTimeoutException e) {
      logService.save(context.getAccountId(),
          aLog()
              .withAppId(context.getAppId())
              .withActivityId(activityId)
              .withHostName(host.getPublicDns())
              .withLogLevel(SUCCESS.equals(commandExecutionStatus) ? INFO : ERROR)
              .withLogLine("Command execution timed out")
              .withCommandUnitName(commandUnit.getName())
              .withExecutionResult(commandExecutionStatus)
              .build());
      throw new WingsException(ErrorCode.SOCKET_CONNECTION_TIMEOUT);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof WingsException) {
        WingsException ex = (WingsException) e.getCause();
        String errorMessage = Misc.getMessage(ex);
        logService.save(context.getAccountId(),
            aLog()
                .withAppId(context.getAppId())
                .withActivityId(activityId)
                .withHostName(host.getPublicDns())
                .withCommandUnitName(commandUnit.getName())
                .withLogLevel(SUCCESS.equals(commandExecutionStatus) ? INFO : ERROR)
                .withLogLine(errorMessage)
                .withExecutionResult(commandExecutionStatus)
                .build());
        throw(WingsException) e.getCause();
      } else {
        logService.save(context.getAccountId(),
            aLog()
                .withAppId(context.getAppId())
                .withActivityId(activityId)
                .withHostName(host.getPublicDns())
                .withLogLevel(SUCCESS.equals(commandExecutionStatus) ? INFO : ERROR)
                .withLogLine("Unknown Error " + e.getCause().getMessage())
                .withCommandUnitName(commandUnit.getName())
                .withExecutionResult(commandExecutionStatus)
                .build());

        throw new WingsException(ErrorCode.UNKNOWN_ERROR, "", e);
      }
    } catch (WingsException e) {
      final List<ResponseMessage> messageList = e.getResponseMessageList(REST_API);
      if (!messageList.isEmpty()) {
        if (messageList.get(0).getCode() == ErrorCode.INVALID_KEY
            || messageList.get(0).getCode() == ErrorCode.INVALID_CREDENTIAL) {
          logService.save(context.getAccountId(),
              aLog()
                  .withAppId(context.getAppId())
                  .withActivityId(activityId)
                  .withHostName(host.getPublicDns())
                  .withLogLevel(SUCCESS.equals(commandExecutionStatus) ? INFO : ERROR)
                  .withLogLine("Command execution failed: invalid key")
                  .withCommandUnitName(commandUnit.getName())
                  .withExecutionResult(commandExecutionStatus)
                  .build());
          throw e;
        }
      } else {
        logger.error("Error while executing command", e);
        logService.save(context.getAccountId(),
            aLog()
                .withAppId(context.getAppId())
                .withActivityId(activityId)
                .withHostName(host.getPublicDns())
                .withLogLevel(SUCCESS.equals(commandExecutionStatus) ? INFO : ERROR)
                .withLogLine("Command execution failed")
                .withCommandUnitName(commandUnit.getName())
                .withExecutionResult(commandExecutionStatus)
                .build());
        throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
      }
    } catch (Exception e) {
      logService.save(context.getAccountId(),
          aLog()
              .withAppId(context.getAppId())
              .withActivityId(activityId)
              .withHostName(host.getPublicDns())
              .withLogLevel(SUCCESS.equals(commandExecutionStatus) ? INFO : ERROR)
              .withLogLine("Command execution failed")
              .withCommandUnitName(commandUnit.getName())
              .withExecutionResult(commandExecutionStatus)
              .build());
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }

    logService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withActivityId(activityId)
            .withHostName(host.getPublicDns())
            .withLogLevel(SUCCESS.equals(commandExecutionStatus) ? INFO : ERROR)
            .withLogLine("Command execution finished with status " + commandExecutionStatus)
            .withCommandUnitName(commandUnit.getName())
            .withExecutionResult(commandExecutionStatus)
            .build());

    commandUnit.setCommandExecutionStatus(commandExecutionStatus);
    return commandExecutionStatus;
  }
}
