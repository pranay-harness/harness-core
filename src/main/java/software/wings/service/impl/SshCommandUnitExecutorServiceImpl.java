package software.wings.service.impl;

import static software.wings.beans.ErrorConstants.UNKNOWN_COMMAND_UNIT_ERROR;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.FAILURE;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CommandUnit;
import software.wings.beans.ExecCommandUnit;
import software.wings.beans.Execution;
import software.wings.beans.Host;
import software.wings.beans.ScpCommandUnit;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.exception.WingsException;
import software.wings.service.intfc.SshCommandUnitExecutorService;

import javax.inject.Singleton;

@Singleton
public class SshCommandUnitExecutorServiceImpl implements SshCommandUnitExecutorService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void execute(Execution execution) {
    SshSessionConfig sshSessionConfig = getSshSessionConfig(execution, execution.getHost());
    for (CommandUnit commandUnit : execution.getCommandUnits()) {
      SshExecutor executor = SshExecutorFactory.getExecutor(sshSessionConfig); // TODO: Reuse executor
      ExecutionResult result;
      switch (commandUnit.getCommandUnitType()) {
        case EXEC:
          ExecCommandUnit execCommandUnit = (ExecCommandUnit) commandUnit;
          result = executor.execute(execCommandUnit.getCommandString());
          break;
        case SCP:
          ScpCommandUnit scpCommandUnit = (ScpCommandUnit) commandUnit;
          result = executor.transferFile(
              scpCommandUnit.getFileId(), scpCommandUnit.getDestinationFilePath(), scpCommandUnit.getFileBucket());
          break;
        default:
          throw new WingsException(
              UNKNOWN_COMMAND_UNIT_ERROR, new Throwable("Unknown command unit: " + commandUnit.getCommandUnitType()));
      }
      commandUnit.setExecutionResult(result);
      if (result == FAILURE) {
        break;
      }
    }
  }

  private SshSessionConfig getSshSessionConfig(Execution execution, Host host) {
    return SshSessionConfig.SshSessionConfigBuilder.aSshSessionConfig()
        .withExecutorType(PASSWORD)
        .withExecutionId(execution.getUuid())
        .withHost(host.getHostName())
        .withPort(22)
        .withUser(execution.getSshUser())
        .withPassword(execution.getSshPassword())
        .build(); // TODO: Expand to add bastion and key based auth
  }
}
