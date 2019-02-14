package software.wings.delegatetasks;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  public CommandExecutionResult run(TaskParameters parameters) {
    return run((ShellScriptParameters) parameters);
  }

  @Override
  public CommandExecutionResult run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  private CommandExecutionResult run(ShellScriptParameters parameters) {
    if (parameters.isExecuteOnDelegate()) {
      ShellExecutor executor = shellExecutorFactory.getExecutor(
          parameters.processExecutorConfig(containerDeploymentDelegateHelper), parameters.getScriptType());
      List<String> items = new ArrayList<>();
      if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
        items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
        items.replaceAll(String::trim);
      }

      return executor.executeCommandString(parameters.getScript(), items);
    }

    switch (parameters.getConnectionType()) {
      case SSH: {
        SshExecutor executor = sshExecutorFactory.getExecutor(ExecutorType.KEY_AUTH);
        try {
          SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
          executor.init(expectedSshConfig);
          List<String> items = new ArrayList<>();
          if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
            items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
            items.replaceAll(String::trim);
          }
          return executor.executeCommandString(parameters.getScript(), items);
        } catch (Exception e) {
          throw new WingsException(e);
        }
      }
      case WINRM: {
        try {
          WinRmSessionConfig winRmSessionConfig = parameters.winrmSessionConfig(encryptionService);
          WinRmExecutor executor = winrmExecutorFactory.getExecutor(winRmSessionConfig);
          List<String> items = new ArrayList<>();
          if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
            items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
            items.replaceAll(String::trim);
          }
          return executor.executeCommandString(parameters.getScript(), items);
        } catch (Exception e) {
          throw new WingsException(e);
        }
      }
      default:
        unhandled(parameters.getConnectionType());
        return CommandExecutionResult.builder()
            .status(FAILURE)
            .errorMessage(format("Unsupported ConnectionType %s", parameters.getConnectionType()))
            .build();
    }
  }
}
