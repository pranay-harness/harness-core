package software.wings.service.impl;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
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
  private static final Logger logger = LoggerFactory.getLogger(SshCommandUnitExecutorServiceImpl.class);
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

    SshSessionConfig sshSessionConfig =
        SshHelperUtil.getSshSessionConfig(host.getPublicDns(), commandUnit.getName(), context, null);
    SshExecutor executor = sshExecutorFactory.getExecutor(sshSessionConfig.getExecutorType()); // TODO: Reuse executor
    executor.init(sshSessionConfig);

    SshCommandExecutionContext sshCommandExecutionContext = new SshCommandExecutionContext(context);
    sshCommandExecutionContext.setSshExecutor(executor);

    injector.injectMembers(commandUnit);

    try {
      commandExecutionStatus = timeLimiter.callWithTimeout(()
                                                               -> commandUnit.execute(sshCommandExecutionContext),
          commandUnit.getCommandExecutionTimeout(), TimeUnit.MILLISECONDS, true);

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
        String errorMessage = Joiner.on(",").join(
            ex.getResponseMessageList()
                .stream()
                .map(responseMessage -> ResponseCodeCache.getInstance().rebuildMessage(responseMessage, ex.getParams()))
                .collect(toList()));
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
      if (!e.getResponseMessageList().isEmpty()) {
        if (e.getResponseMessageList().get(0).getCode() == ErrorCode.INVALID_KEY
            || e.getResponseMessageList().get(0).getCode() == ErrorCode.INVALID_CREDENTIAL) {
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
        throw new WingsException(ErrorCode.UNKNOWN_ERROR);
      }
    } catch (Exception e) {
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
      throw new WingsException(ErrorCode.UNKNOWN_ERROR);
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
