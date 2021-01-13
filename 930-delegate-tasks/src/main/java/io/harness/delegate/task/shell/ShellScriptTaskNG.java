package io.harness.delegate.task.shell;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.k8s.KubernetesContainerService;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ShellExecutorConfig;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class ShellScriptTaskNG extends AbstractDelegateRunnableTask {
  public static final String COMMAND_UNIT = "Execute";

  @Inject private ShellExecutorFactoryNG shellExecutorFactory;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;

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
    }
    return ShellScriptTaskResponseNG.builder().build();
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
