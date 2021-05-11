package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Scale;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.k8s.K8sCommandUnitConstants.WrapUp;
import static io.harness.k8s.model.KubernetesResourceId.createKubernetesResourceIdFromNamespaceKindName;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Cyan;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.exception.KubernetesExceptionExplanation;
import io.harness.delegate.k8s.exception.KubernetesExceptionHints;
import io.harness.delegate.k8s.exception.KubernetesExceptionMessage;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sScaleRequest;
import io.harness.delegate.task.k8s.K8sScaleResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDP)
@NoArgsConstructor
@Slf4j
public class K8sScaleRequestHandler extends K8sRequestHandler {
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  private Kubectl client;
  private KubernetesResourceId resourceIdToScale;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  private int targetReplicaCount;

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8SDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(k8sDeployRequest instanceof K8sScaleRequest)) {
      throw new InvalidArgumentsException(Pair.of("k8sDeployRequest", "Must be instance of K8sScaleRequest"));
    }

    K8sScaleRequest k8sScaleRequest = (K8sScaleRequest) k8sDeployRequest;

    KubernetesConfig kubernetesConfig =
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(k8sScaleRequest.getK8sInfraDelegateConfig());

    startNewCommandUnit(Init, true);
    init(k8sScaleRequest, k8SDelegateTaskParams, kubernetesConfig.getNamespace(), getCurrentLogCallback());

    if (resourceIdToScale == null) {
      return getSuccessResponse(K8sScaleResponse.builder().build());
    }

    long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(k8sScaleRequest.getTimeoutIntervalInMin());
    startNewCommandUnit(Scale, true);
    getCurrentLogCallback().saveExecutionLog("Fetching existing pods before scale.");
    List<K8sPod> beforePodList = k8sTaskHelperBase.getPodDetails(kubernetesConfig, resourceIdToScale.getNamespace(),
        k8sScaleRequest.getReleaseName(), steadyStateTimeoutInMillis);

    k8sTaskHelperBase.scale(
        client, k8SDelegateTaskParams, resourceIdToScale, targetReplicaCount, getCurrentLogCallback(), true);

    if (!k8sScaleRequest.isSkipSteadyStateCheck()) {
      startNewCommandUnit(WaitForSteadyState, true);
      if (!k8sTaskHelperBase.doStatusCheck(
              client, resourceIdToScale, k8SDelegateTaskParams, getCurrentLogCallback(), false)) {
        throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.WAIT_FOR_STEADY_STATE_FAILED,
            KubernetesExceptionExplanation.WAIT_FOR_STEADY_STATE_FAILED
                + ". Managed workload: " + resourceIdToScale.namespaceKindNameRef(),
            new KubernetesTaskException(KubernetesExceptionMessage.WAIT_FOR_STEADY_STATE_FAILED));
      }
    }

    startNewCommandUnit(WrapUp, true);

    getCurrentLogCallback().saveExecutionLog("Fetching existing pods after scale.");
    List<K8sPod> afterPodList = k8sTaskHelperBase.getPodDetails(kubernetesConfig, resourceIdToScale.getNamespace(),
        k8sScaleRequest.getReleaseName(), steadyStateTimeoutInMillis);

    K8sScaleResponse k8sScaleResponse =
        K8sScaleResponse.builder().k8sPodList(k8sTaskHelperBase.tagNewPods(beforePodList, afterPodList)).build();

    getCurrentLogCallback().saveExecutionLog("\nDone.", INFO, SUCCESS);

    return getSuccessResponse(k8sScaleResponse);
  }

  @Override
  public boolean isErrorFrameworkSupported() {
    return true;
  }

  private K8sDeployResponse getSuccessResponse(K8sScaleResponse k8sScaleResponse) {
    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(k8sScaleResponse)
        .build();
  }

  @VisibleForTesting
  void init(K8sScaleRequest request, K8sDelegateTaskParams k8sDelegateTaskParams, String namespace,
      LogCallback executionLogCallback) throws Exception {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    if (StringUtils.isEmpty(request.getWorkload())) {
      executionLogCallback.saveExecutionLog("\nNo Workload found to scale.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);
      return;
    }

    resourceIdToScale = createKubernetesResourceIdFromNamespaceKindName(request.getWorkload());
    if (resourceIdToScale == null) {
      throw new KubernetesTaskException("Missing resource id to scale");
    }

    executionLogCallback.saveExecutionLog(
        color("\nWorkload to scale is: ", White, Bold) + color(resourceIdToScale.namespaceKindNameRef(), Cyan, Bold));

    if (isBlank(resourceIdToScale.getNamespace())) {
      resourceIdToScale.setNamespace(namespace);
    }

    executionLogCallback.saveExecutionLog("\nQuerying current replicas");
    Integer currentReplicas = k8sTaskHelperBase.getCurrentReplicas(client, resourceIdToScale, k8sDelegateTaskParams);
    executionLogCallback.saveExecutionLog("Current replica count is " + currentReplicas);

    if (request.getInstanceUnitType() == null) {
      throw NestedExceptionUtils.hintWithExplanationException(KubernetesExceptionHints.SCALE_INSTANCE_UNIT_TYPE_MISSING,
          KubernetesExceptionExplanation.SCALE_INSTANCE_UNIT_TYPE_MISSING,
          new KubernetesTaskException("Failed to initialize scale task"));
    }

    switch (request.getInstanceUnitType()) {
      case COUNT:
        targetReplicaCount = request.getInstances();
        break;

      case PERCENTAGE:
        Integer maxInstances;
        if (request.getMaxInstances() != null && request.getMaxInstances().isPresent()) {
          maxInstances = request.getMaxInstances().get();
        } else {
          maxInstances = currentReplicas;
        }

        if (maxInstances == null) {
          throw NestedExceptionUtils.hintWithExplanationException(
              KubernetesExceptionHints.PERCENTAGE_CURRENT_REPLICA_NOT_FOUND,
              format(KubernetesExceptionExplanation.PERCENTAGE_CURRENT_REPLICA_NOT_FOUND, resourceIdToScale.getKind(),
                  resourceIdToScale.getName()),
              new KubernetesTaskException(format(KubernetesExceptionMessage.PERCENTAGE_CURRENT_REPLICA_NOT_FOUND,
                  resourceIdToScale.getKind(), resourceIdToScale.getName(), resourceIdToScale.getNamespace())));
        }

        targetReplicaCount = (int) Math.round(request.getInstances() * maxInstances / 100.0);
        break;

      default:
        unhandled(request.getInstanceUnitType());
    }

    executionLogCallback.saveExecutionLog("Target replica count is " + targetReplicaCount);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);
  }
}
