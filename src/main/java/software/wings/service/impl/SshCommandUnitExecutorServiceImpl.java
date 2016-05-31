package software.wings.service.impl;

import static software.wings.beans.ErrorConstants.UNKNOWN_COMMAND_UNIT_ERROR;
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
import software.wings.exception.WingsException;
import software.wings.service.intfc.CommandUnitExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SshCommandUnitExecutorServiceImpl implements CommandUnitExecutorService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private SshExecutorFactory sshExecutorFactory;

  @Override
  public ExecutionResult execute(Host host, CommandUnit commandUnit) {
    SshSessionConfig sshSessionConfig = getSshSessionConfig(host, commandUnit.getExecutionId());
    SshExecutor executor = sshExecutorFactory.getExecutor(sshSessionConfig); // TODO: Reuse executor
    ExecutionResult executionResult;
    switch (commandUnit.getCommandUnitType()) {
      case EXEC:
        ExecCommandUnit execCommandUnit = (ExecCommandUnit) commandUnit;
        executionResult = executor.execute(execCommandUnit.getCommandString());
        break;
      case COPY:
        CopyCommandUnit scpCommandUnit = (CopyCommandUnit) commandUnit;
        executionResult = executor.transferFile(
            scpCommandUnit.getFileId(), scpCommandUnit.getDestinationFilePath(), scpCommandUnit.getFileBucket());
        break;
      default:
        throw new WingsException(UNKNOWN_COMMAND_UNIT_ERROR,
            new Throwable("Unknown command unit over ssh channel: " + commandUnit.getCommandUnitType()));
    }
    commandUnit.setExecutionResult(executionResult);
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
