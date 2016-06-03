package software.wings.service.impl;

import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SU_APP_USER;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;
import static software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder.aSshSessionConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.CommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.CopyCommandUnit;
import software.wings.beans.ExecCommandUnit;
import software.wings.beans.Host;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionCredential;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder;
import software.wings.service.intfc.CommandUnitExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SshCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private SshExecutorFactory sshExecutorFactory;

  @Inject
  public SshCommandUnitExecutorServiceImpl(SshExecutorFactory sshExecutorFactory) {
    this.sshExecutorFactory = sshExecutorFactory;
  }

  @Override
  public ExecutionResult execute(Host host, CommandUnit commandUnit, String activityId) {
    if (commandUnit instanceof ExecCommandUnit) {
      return execute(host, commandUnit, activityId, SupportedOp.EXEC);
    } else {
      return execute(host, commandUnit, activityId, SupportedOp.SCP);
    }
  }

  private ExecutionResult execute(Host host, CommandUnit commandUnit, String activityId, SupportedOp op) {
    SshSessionConfig sshSessionConfig = getSshSessionConfig(host, activityId);
    SshExecutor executor = sshExecutorFactory.getExecutor(sshSessionConfig.getExecutorType()); // TODO: Reuse executor
    executor.init(sshSessionConfig);
    ExecutionResult executionResult;
    executionResult = executeByCommandType(executor, commandUnit, op);
    commandUnit.setExecutionResult(executionResult);
    return executionResult;
  }

  private ExecutionResult executeByCommandType(SshExecutor executor, CommandUnit commandUnit, SupportedOp op) {
    ExecutionResult executionResult;
    if (op.equals(SupportedOp.EXEC)) {
      ExecCommandUnit execCommandUnit = (ExecCommandUnit) commandUnit;
      executionResult = executor.execute(execCommandUnit.getCommandString());
    } else {
      CopyCommandUnit copyCommandUnit = (CopyCommandUnit) commandUnit;
      executionResult = executor.transferFile(
          copyCommandUnit.getFileId(), copyCommandUnit.getDestinationFilePath(), copyCommandUnit.getFileBucket());
    }
    return executionResult;
  }

  private SshSessionConfig getSshSessionConfig(Host host, String executionId) {
    ExecutorType executorType = getExecutorType(host);
    SshSessionConfigBuilder builder =
        aSshSessionConfig().withExecutionId(executionId).withExecutorType(executorType).withHost(host.getHostName());

    if (host.getHostConnectionCredential() != null) {
      HostConnectionCredential credential = host.getHostConnectionCredential();
      builder.withUserName(credential.getSshUser())
          .withPassword(credential.getSshPassword())
          .withSudoAppName(credential.getAppUser())
          .withSudoAppPassword(credential.getAppUserPassword());
    }

    if (executorType.equals(KEY_AUTH)) {
      HostConnectionAttributes hostConnectionAttrs = (HostConnectionAttributes) host.getHostConnAttr().getValue();
      builder.withKey(hostConnectionAttrs.getKey()).withKeyPassphrase(hostConnectionAttrs.getKeyPassphrase());
    }

    if (host.getBastionConnAttr() != null) {
      BastionConnectionAttributes bastionAttrs = (BastionConnectionAttributes) host.getBastionConnAttr().getValue();
      builder.withJumpboxConfig(aSshSessionConfig()
                                    .withHost(bastionAttrs.getHostName())
                                    .withKey(bastionAttrs.getKey())
                                    .withKeyPassphrase(bastionAttrs.getKeyPassphrase())
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

  private enum SupportedOp { EXEC, SCP }
}
