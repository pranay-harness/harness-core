package io.harness.delegate.task.shell;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.k8s.KubernetesContainerService;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.ShellExecutorConfig;
import io.harness.shell.SshSessionConfig;
import io.harness.shell.SshSessionManager;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class ShellScriptTaskNG extends AbstractDelegateRunnableTask {
  public static final String COMMAND_UNIT = "Execute";

  @Inject private ShellExecutorFactoryNG shellExecutorFactory;
  @Inject private SshExecutorFactoryNG sshExecutorFactoryNG;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;

  public ShellScriptTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ShellScriptTaskParametersNG taskParameters = (ShellScriptTaskParametersNG) parameters;

    if (taskParameters.isExecuteOnDelegate()) {
      ShellExecutorConfig shellExecutorConfig = getShellExecutorConfig(taskParameters);
      ScriptProcessExecutor executor =
          shellExecutorFactory.getExecutor(shellExecutorConfig, this.getLogStreamingTaskClient());
      // TODO: check later
      // if (taskParameters.isLocalOverrideFeatureFlag()) {
      //   taskParameters.setScript(delegateLocalConfigService.replacePlaceholdersWithLocalConfig(taskParameters.getScript()));
      // }
      ExecuteCommandResponse executeCommandResponse =
          executor.executeCommandString(taskParameters.getScript(), taskParameters.getOutputVars());
      return ShellScriptTaskResponseNG.builder().executeCommandResponse(executeCommandResponse).build();
    } else {
      try {
        SshSessionConfig sshSessionConfig = getSshSessionConfig(taskParameters);
        ScriptSshExecutor executor =
            sshExecutorFactoryNG.getExecutor(sshSessionConfig, this.getLogStreamingTaskClient());
        ExecuteCommandResponse executeCommandResponse =
            executor.executeCommandString(taskParameters.getScript(), taskParameters.getOutputVars());
        return ShellScriptTaskResponseNG.builder().executeCommandResponse(executeCommandResponse).build();
      } catch (Exception e) {
        log.error("Bash Script Failed to execute.", e);
        return ShellScriptTaskResponseNG.builder()
            .status(CommandExecutionStatus.FAILURE)
            .errorMessage("Bash Script Failed to execute. Reason: " + e.getMessage())
            .build();
      } finally {
        SshSessionManager.evictAndDisconnectCachedSession(taskParameters.getExecutionId(), taskParameters.getHost());
      }
    }
  }

  private SshSessionConfig getSshSessionConfig(ShellScriptTaskParametersNG taskParameters) {
    SshSessionConfig sshSessionConfig = sshSessionConfigMapper.getSSHSessionConfig(
        taskParameters.getSshKeySpecDTO(), taskParameters.getEncryptionDetails());

    sshSessionConfig.setAccountId(taskParameters.getAccountId());
    sshSessionConfig.setExecutionId(taskParameters.getExecutionId());
    sshSessionConfig.setHost(taskParameters.getHost());
    sshSessionConfig.setWorkingDirectory(taskParameters.getWorkingDirectory());
    sshSessionConfig.setCommandUnitName(COMMAND_UNIT);
    return sshSessionConfig;
  }

  private ShellExecutorConfig getShellExecutorConfig(ShellScriptTaskParametersNG taskParameters) {
    String kubeConfigFileContent = taskParameters.getK8sInfraDelegateConfig() != null
        ? containerDeploymentDelegateBaseHelper.getKubeconfigFileContent(taskParameters.getK8sInfraDelegateConfig())
        : "";

    return ShellExecutorConfig.builder()
        .accountId(taskParameters.getAccountId())
        .executionId(taskParameters.getExecutionId())
        .commandUnitName(COMMAND_UNIT)
        .workingDirectory(taskParameters.getWorkingDirectory())
        .environment(taskParameters.getEnvironmentVariables())
        .kubeConfigContent(kubeConfigFileContent)
        .scriptType(taskParameters.getScriptType())
        .build();
  }
}
