package software.wings.delegatetasks;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static software.wings.beans.command.CommandExecutionResult.Builder.aCommandExecutionResult;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.local.executors.ShellExecutor;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmExecutor;
import software.wings.core.winrm.executors.WinRmExecutorFactory;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.intfc.security.EncryptionService;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShellScriptTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(ShellScriptTask.class);

  @Inject private SshExecutorFactory sshExecutorFactory;
  @Inject private WinRmExecutorFactory winrmExecutorFactory;
  @Inject private ShellExecutorFactory shellExecutorFactory;
  @Inject private EncryptionService encryptionService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  public ShellScriptTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public CommandExecutionResult run(Object[] parameters) {
    return run((ShellScriptParameters) parameters[0]);
  }

  private CommandExecutionResult run(ShellScriptParameters parameters) {
    if (parameters.isExecuteOnDelegate()) {
      ShellExecutor executor = shellExecutorFactory.getExecutor(
          parameters.processExecutorConfig(containerDeploymentDelegateHelper), parameters.getScriptType());
      CommandExecutionStatus commandExecutionStatus =
          executor.executeCommandString(parameters.getScript(), new StringBuffer());

      return aCommandExecutionResult().withStatus(commandExecutionStatus).build();
    }

    switch (parameters.getConnectionType()) {
      case SSH: {
        SshExecutor executor = sshExecutorFactory.getExecutor(ExecutorType.KEY_AUTH);
        try {
          SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
          executor.init(expectedSshConfig);

          CommandExecutionStatus commandExecutionStatus = executor.executeCommandString(parameters.getScript(), false);

          return aCommandExecutionResult().withStatus(commandExecutionStatus).build();
        } catch (Exception e) {
          throw new WingsException(e);
        }
      }
      case WINRM: {
        try {
          WinRmSessionConfig winRmSessionConfig = parameters.winrmSessionConfig(encryptionService);
          WinRmExecutor executor = winrmExecutorFactory.getExecutor(winRmSessionConfig);

          CommandExecutionStatus commandExecutionStatus =
              executor.executeCommandString(parameters.getScript(), new StringBuffer());

          return aCommandExecutionResult().withStatus(commandExecutionStatus).build();
        } catch (Exception e) {
          throw new WingsException(e);
        }
      }
      default:
        unhandled(parameters.getConnectionType());
        return aCommandExecutionResult()
            .withStatus(FAILURE)
            .withErrorMessage(format("Unsupported ConnectionType %s", parameters.getConnectionType()))
            .build();
    }
  }
}
