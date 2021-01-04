package software.wings.delegatetasks;

import static software.wings.sm.states.KubernetesSwapServiceSelectors.KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.K8sSwapServiceSelectorsBaseHandler;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.WingsException;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logging.Misc;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.KubernetesSwapServiceSelectorsParams;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.sm.states.KubernetesSwapServiceSelectorsResponse;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
@Slf4j
public class KubernetesSwapServiceSelectorsTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private K8sSwapServiceSelectorsBaseHandler k8sSwapServiceSelectorsBaseHandler;

  public KubernetesSwapServiceSelectorsTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public KubernetesSwapServiceSelectorsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public KubernetesSwapServiceSelectorsResponse run(Object[] parameters) {
    KubernetesSwapServiceSelectorsParams kubernetesSwapServiceSelectorsParams =
        (KubernetesSwapServiceSelectorsParams) parameters[0];

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(delegateLogService,
        kubernetesSwapServiceSelectorsParams.getAccountId(), kubernetesSwapServiceSelectorsParams.getAppId(),
        kubernetesSwapServiceSelectorsParams.getActivityId(), kubernetesSwapServiceSelectorsParams.getCommandName());

    executionLogCallback.saveExecutionLog(
        String.format("Begin execution of command %s", KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME), LogLevel.INFO);

    try {
      KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(
          kubernetesSwapServiceSelectorsParams.getContainerServiceParams());

      boolean success = k8sSwapServiceSelectorsBaseHandler.swapServiceSelectors(kubernetesConfig,
          kubernetesSwapServiceSelectorsParams.getService1(), kubernetesSwapServiceSelectorsParams.getService2(),
          executionLogCallback);

      if (!success) {
        return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
      }

      return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build();
    } catch (WingsException e) {
      Misc.logAllMessages(e, executionLogCallback);
      throw e;
    } catch (Exception e) {
      log.error("Exception in KubernetesSwapServiceSelectors", e);
      Misc.logAllMessages(e, executionLogCallback);
      executionLogCallback.saveExecutionLog(
          "Exception occurred in kubernetesSwapServiceSelectors", LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
    }
  }
}
