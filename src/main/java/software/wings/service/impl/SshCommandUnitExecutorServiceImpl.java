package software.wings.service.impl;

import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.CopyCommandUnit;
import software.wings.beans.ExecCommandUnit;
import software.wings.beans.Host;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.service.intfc.CommandUnitExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SshCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private SshExecutorFactory sshExecutorFactory;

  private enum SupportedOp { EXEC, SCP }

  @Override
  public ExecutionResult execute(Host host, ExecCommandUnit commandUnit) {
    return execute(host, commandUnit, SupportedOp.EXEC);
  }

  @Override
  public ExecutionResult execute(Host host, CopyCommandUnit commandUnit) {
    return execute(host, commandUnit, SupportedOp.SCP);
  }

  private ExecutionResult execute(Host host, CommandUnit commandUnit, SupportedOp op) {
    SshSessionConfig sshSessionConfig = getSshSessionConfig(host, commandUnit.getExecutionId());
    SshExecutor executor = sshExecutorFactory.getExecutor(sshSessionConfig); // TODO: Reuse executor
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
    return SshSessionConfig.SshSessionConfigBuilder.aSshSessionConfig()
        .withExecutorType(PASSWORD)
        .withExecutionId(executionId)
        .withHost(host.getHostName())
        .withPort(22)
        .withUserName(host.getHostConnectionCredential().getSshUser())
        .withPassword(host.getHostConnectionCredential().getSshPassword())
        .build(); // TODO: Expand to add bastion and key based auth
  }
}
