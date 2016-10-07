package software.wings.service.impl;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SU_APP_USER;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.command.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import com.google.common.base.Joiner;
import com.google.inject.Injector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.ErrorCodes;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.beans.command.SshCommandExecutionContext;
import software.wings.beans.infrastructure.Host;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.core.ssh.executors.AbstractSshExecutor;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.ssh.executors.SshSessionConfig.Builder;
import software.wings.exception.WingsException;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.LogService;
import software.wings.utils.TimeoutManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;
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
  protected LogService logService;
  /**
   * The Executor service.
   */
  @Inject ExecutorService executorService;

  @Inject private TimeoutManager timeoutManager;

  @Inject private Injector injector;

  private SshExecutorFactory sshExecutorFactory;

  /**
   * Instantiates a new ssh command unit executor service impl.
   *
   * @param sshExecutorFactory the ssh executor factory
   * @param logService         the log service
   */
  @Inject
  public SshCommandUnitExecutorServiceImpl(SshExecutorFactory sshExecutorFactory, LogService logService) {
    this.sshExecutorFactory = sshExecutorFactory;
    this.logService = logService;
  }

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
    logService.save(aLog()
                        .withAppId(context.getAppId())
                        .withHostName(host.getHostName())
                        .withActivityId(activityId)
                        .withLogLevel(INFO)
                        .withCommandUnitName(commandUnit.getName())
                        .withLogLine(format("Begin execution of command: %s", commandUnit.getName()))
                        .build());

    ExecutionResult executionResult = FAILURE;

    try {
      SshSessionConfig sshSessionConfig = getSshSessionConfig(host, context, commandUnit);
      SshExecutor executor = sshExecutorFactory.getExecutor(sshSessionConfig.getExecutorType()); // TODO: Reuse executor
      executor.init(sshSessionConfig);

      SshCommandExecutionContext sshCommandExecutionContext = new SshCommandExecutionContext(context);
      sshCommandExecutionContext.setSshExecutor(executor);

      injector.injectMembers(commandUnit);

      Future<ExecutionResult> executionResultFuture =
          executorService.submit(() -> commandUnit.execute(sshCommandExecutionContext));

      try {
        executionResult = executionResultFuture.get(
            commandUnit.getCommandExecutionTimeout(), TimeUnit.MILLISECONDS); // TODO: Improve logging
      } catch (InterruptedException | TimeoutException e) {
        logService.save(aLog()
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
          logService.save(aLog()
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
          logService.save(aLog()
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
      }

      logService.save(aLog()
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

    } catch (Exception ex) {
      logger.error("Command execution failed with error " + ex);
      logService.save(aLog()
                          .withAppId(context.getAppId())
                          .withActivityId(activityId)
                          .withHostName(host.getHostName())
                          .withLogLevel(SUCCESS.equals(executionResult) ? INFO : ERROR)
                          .withLogLine("Command execution finished with status " + executionResult)
                          .withCommandUnitName(commandUnit.getName())
                          .withExecutionResult(executionResult)
                          .build());
      throw ex;
    }
  }

  private SshSessionConfig getSshSessionConfig(Host host, CommandExecutionContext context, CommandUnit commandUnit) {
    ExecutorType executorType = getExecutorType(host);

    SSHExecutionCredential sshExecutionCredential = (SSHExecutionCredential) context.getExecutionCredential();

    Builder builder = aSshSessionConfig()
                          .withAppId(host.getAppId())
                          .withExecutionId(context.getActivityId())
                          .withExecutorType(executorType)
                          .withHost(host.getHostName())
                          .withCommandUnitName(commandUnit.getName())
                          .withUserName(sshExecutionCredential.getSshUser())
                          .withPassword(sshExecutionCredential.getSshPassword())
                          .withSudoAppName(sshExecutionCredential.getAppAccount())
                          .withSudoAppPassword(sshExecutionCredential.getAppAccountPassword())
                          .withKeyPassphrase(sshExecutionCredential.getKeyPassphrase());

    if (executorType.equals(KEY_AUTH)) {
      HostConnectionAttributes hostConnectionAttrs = (HostConnectionAttributes) host.getHostConnAttr().getValue();
      builder.withKey(hostConnectionAttrs.getKey())
          .withUserName(hostConnectionAttrs.getUserName())
          .withKeyName(host.getHostConnAttr().getUuid())
          .withPassword(null);
    }

    if (host.getBastionConnAttr() != null) {
      BastionConnectionAttributes bastionAttrs = (BastionConnectionAttributes) host.getBastionConnAttr().getValue();
      builder.withBastionHostConfig(aSshSessionConfig()
                                        .withHost(bastionAttrs.getHostName())
                                        .withKey(bastionAttrs.getKey())
                                        .withKeyName(host.getBastionConnAttr().getUuid())
                                        .withUserName(bastionAttrs.getUserName())
                                        .build());
    }
    return builder.build();
  }

  private ExecutorType getExecutorType(Host host) {
    ExecutorType executorType;
    if (host.getBastionConnAttr() != null) {
      executorType = BASTION_HOST;
    } else {
      AccessType accessType = ((HostConnectionAttributes) host.getHostConnAttr().getValue()).getAccessType();
      if (accessType.equals(AccessType.KEY) || accessType.equals(KEY_SU_APP_USER)
          || accessType.equals(KEY_SUDO_APP_USER)) {
        executorType = KEY_AUTH;
      } else {
        executorType = PASSWORD_AUTH;
      }
    }
    return executorType;
  }
}
