package io.harness.delegate.k8s;

import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Rollback;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;

import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.beans.K8sRollingRollbackHandlerConfig;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingRollbackDeployRequest;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Slf4j
public class K8sRollingRollbackRequestHandler extends K8sRequestHandler {
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject K8sRollingRollbackBaseHandler rollbackBaseHandler;

  private final K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(k8sDeployRequest instanceof K8sRollingRollbackDeployRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sDeployRequest", "Must be instance of K8sRollingRollbackDeployRequest"));
    }

    K8sRollingRollbackDeployRequest k8sRollingRollbackDeployRequest =
        (K8sRollingRollbackDeployRequest) k8sDeployRequest;
    boolean success = init(k8sRollingRollbackDeployRequest, k8sDelegateTaskParams,
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Init, true, commandUnitsProgress));
    if (!success) {
      return getGenericFailureResponse(null);
    }

    success = rollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams,
        k8sRollingRollbackDeployRequest.getReleaseNumber(),
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Rollback, true, commandUnitsProgress));
    if (!success) {
      return getGenericFailureResponse(null);
    }

    rollbackBaseHandler.steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams,
        k8sRollingRollbackDeployRequest.getTimeoutIntervalInMin(),
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, WaitForSteadyState, true, commandUnitsProgress));
    rollbackBaseHandler.postProcess(rollbackHandlerConfig, k8sRollingRollbackDeployRequest.getReleaseName());
    return K8sDeployResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }

  private boolean init(K8sRollingRollbackDeployRequest rollbackRequest, K8sDelegateTaskParams k8sDelegateTaskParams,
      LogCallback logCallback) throws IOException {
    logCallback.saveExecutionLog("Initializing..\n");

    rollbackHandlerConfig.setKubernetesConfig(
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(rollbackRequest.getK8sInfraDelegateConfig()));
    rollbackHandlerConfig.setClient(
        Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath()));

    return rollbackBaseHandler.init(rollbackHandlerConfig, rollbackRequest.getReleaseName(), logCallback);
  }

  @VisibleForTesting
  K8sRollingRollbackHandlerConfig getRollbackHandlerConfig() {
    return rollbackHandlerConfig;
  }
}
