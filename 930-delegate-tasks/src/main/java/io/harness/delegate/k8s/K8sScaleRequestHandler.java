package io.harness.delegate.k8s;

import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Scale;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.k8s.model.KubernetesResourceId.createKubernetesResourceIdFromNamespaceKindName;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.validation.Validator.nullCheckForInvalidRequest;

import static software.wings.beans.LogColor.Cyan;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sScaleRequest;
import io.harness.delegate.task.k8s.K8sScaleResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
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

    boolean success = init(k8sScaleRequest, k8SDelegateTaskParams, kubernetesConfig.getNamespace(),
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Init, true, commandUnitsProgress));
    if (!success) {
      return getFailureResponse();
    }

    long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(k8sScaleRequest.getTimeoutIntervalInMin());

    List<K8sPod> beforePodList = k8sTaskHelperBase.getPodDetails(kubernetesConfig, resourceIdToScale.getNamespace(),
        k8sScaleRequest.getReleaseName(), steadyStateTimeoutInMillis);

    success = k8sTaskHelperBase.scale(client, k8SDelegateTaskParams, resourceIdToScale, targetReplicaCount,
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Scale, true, commandUnitsProgress));
    if (!success) {
      return getFailureResponse();
    }

    if (!k8sScaleRequest.isSkipSteadyStateCheck()) {
      success = k8sTaskHelperBase.doStatusCheck(client, resourceIdToScale, k8SDelegateTaskParams,
          k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, WaitForSteadyState, true, commandUnitsProgress));

      if (!success) {
        return getFailureResponse();
      }
    }

    List<K8sPod> afterPodList = k8sTaskHelperBase.getPodDetails(kubernetesConfig, resourceIdToScale.getNamespace(),
        k8sScaleRequest.getReleaseName(), steadyStateTimeoutInMillis);

    K8sScaleResponse k8sScaleResponse =
        K8sScaleResponse.builder().k8sPodList(k8sTaskHelperBase.tagNewPods(beforePodList, afterPodList)).build();

    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(k8sScaleResponse)
        .build();
  }

  private K8sDeployResponse getFailureResponse() {
    K8sScaleResponse k8sScaleResponse = K8sScaleResponse.builder().build();
    return getGenericFailureResponse(k8sScaleResponse);
  }

  @VisibleForTesting
  boolean init(K8sScaleRequest request, K8sDelegateTaskParams k8sDelegateTaskParams, String namespace,
      LogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    try {
      client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

      if (StringUtils.isEmpty(request.getWorkload())) {
        executionLogCallback.saveExecutionLog("\nNo Workload found to scale.");
        executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);
        return true;
      }

      resourceIdToScale = createKubernetesResourceIdFromNamespaceKindName(request.getWorkload());
      if (resourceIdToScale == null) {
        return false;
      }

      executionLogCallback.saveExecutionLog(
          color("\nWorkload to scale is: ", White, Bold) + color(resourceIdToScale.namespaceKindNameRef(), Cyan, Bold));

      if (isBlank(resourceIdToScale.getNamespace())) {
        resourceIdToScale.setNamespace(namespace);
      }

      executionLogCallback.saveExecutionLog("\nQuerying current replicas");
      Integer currentReplicas = k8sTaskHelperBase.getCurrentReplicas(client, resourceIdToScale, k8sDelegateTaskParams);
      executionLogCallback.saveExecutionLog("Current replica count is " + currentReplicas);

      switch (request.getInstanceUnitType()) {
        case COUNT:
          targetReplicaCount = request.getInstances();
          break;

        case PERCENTAGE:
          Integer maxInstances;
          if (request.getMaxInstances().isPresent()) {
            maxInstances = request.getMaxInstances().get();
          } else {
            maxInstances = currentReplicas;
          }
          nullCheckForInvalidRequest(maxInstances,
              format("Could not get current replica count for workload %s/%s in namespace %s",
                  resourceIdToScale.getKind(), resourceIdToScale.getName(), resourceIdToScale.getNamespace()),
              USER);
          targetReplicaCount = (int) Math.round(request.getInstances() * maxInstances / 100.0);
          break;

        default:
          unhandled(request.getInstanceUnitType());
      }

      executionLogCallback.saveExecutionLog("Target replica count is " + targetReplicaCount);

      executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);

      return true;
    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }
}
