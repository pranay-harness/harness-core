package software.wings.delegatetasks;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.service.ExecutionConfigOverrideFromFileOnDelegate;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.core.ssh.executors.ScriptExecutor;
import software.wings.core.ssh.executors.ScriptProcessExecutor;
import software.wings.core.ssh.executors.ScriptSshExecutor;
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

@Slf4j
public class ShellScriptTask extends AbstractDelegateRunnableTask {
  @Inject private SshExecutorFactory sshExecutorFactory;
  @Inject private WinRmExecutorFactory winrmExecutorFactory;
  @Inject private ShellExecutorFactory shellExecutorFactory;
  @Inject private EncryptionService encryptionService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private ExecutionConfigOverrideFromFileOnDelegate delegateLocalConfigService;

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
      ScriptProcessExecutor executor =
          shellExecutorFactory.getExecutor(parameters.processExecutorConfig(containerDeploymentDelegateHelper));
      List<String> items = new ArrayList<>();
      if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
        items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
        items.replaceAll(String::trim);
      }
      parameters.setScript(delegateLocalConfigService.replacePlaceholdersWithLocalConfig(parameters.getScript()));
      return executor.executeCommandString(parameters.getScript(), items);
    }

    switch (parameters.getConnectionType()) {
      case SSH: {
        try {
          SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
          ScriptExecutor executor = sshExecutorFactory.getExecutor(expectedSshConfig);
          List<String> items = new ArrayList<>();
          if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
            items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
            items.replaceAll(String::trim);
          }
          return executor.executeCommandString(parameters.getScript(), items);
        } catch (Exception e) {
          throw new WingsException(e);
        } finally {
          ScriptSshExecutor.evictAndDisconnectCachedSession(parameters.getActivityId(), parameters.getHost());
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
