package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.model.KubernetesResourceId.createKubernetesResourceIdFromKindName;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Scale;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesResourceId;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sScaleTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sScaleResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.utils.Misc;

@NoArgsConstructor
public class K8sScaleTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sScaleTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private Kubectl client;
  private KubernetesResourceId resourceIdToScale;
  private int targetReplicaCount;
  private K8sScaleResponse k8sScaleResponse;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sScaleTaskParameters)) {
      throw new InvalidArgumentsException(Pair.of("k8sTaskParameters", "Must be instance of K8sScaleTaskParameters"));
    }

    K8sScaleTaskParameters k8sScaleTaskParameters = (K8sScaleTaskParameters) k8sTaskParameters;

    k8sScaleResponse = K8sScaleResponse.builder().build();

    boolean success = init(k8sScaleTaskParameters, k8sDelegateTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sScaleTaskParameters.getAccountId(),
            k8sScaleTaskParameters.getAppId(), k8sScaleTaskParameters.getActivityId(), Init));

    if (!success) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sScaleResponse, CommandExecutionStatus.FAILURE);
    }

    if (resourceIdToScale == null) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sScaleResponse, CommandExecutionStatus.SUCCESS);
    }

    success = k8sTaskHelper.scale(client, k8sDelegateTaskParams, resourceIdToScale, targetReplicaCount,
        new ExecutionLogCallback(delegateLogService, k8sScaleTaskParameters.getAccountId(),
            k8sScaleTaskParameters.getAppId(), k8sScaleTaskParameters.getActivityId(), Scale));

    if (!success) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sScaleResponse, CommandExecutionStatus.FAILURE);
    }

    if (!k8sScaleTaskParameters.isSkipSteadyStateCheck()) {
      success = k8sTaskHelper.doStatusCheck(client, resourceIdToScale, k8sDelegateTaskParams,
          new ExecutionLogCallback(delegateLogService, k8sTaskParameters.getAccountId(), k8sTaskParameters.getAppId(),
              k8sTaskParameters.getActivityId(), WaitForSteadyState));

      if (!success) {
        return k8sTaskHelper.getK8sTaskExecutionResponse(k8sScaleResponse, CommandExecutionStatus.FAILURE);
      }
    }

    return k8sTaskHelper.getK8sTaskExecutionResponse(k8sScaleResponse, CommandExecutionStatus.SUCCESS);
  }

  private boolean init(K8sScaleTaskParameters k8sScaleTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    try {
      client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

      if (StringUtils.isEmpty(k8sScaleTaskParameters.getWorkload())) {
        executionLogCallback.saveExecutionLog("\nNo Workload found to scale.");
        executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);
        return true;
      }

      resourceIdToScale = createKubernetesResourceIdFromKindName(k8sScaleTaskParameters.getWorkload());

      executionLogCallback.saveExecutionLog("\nWorkload to scale is: " + resourceIdToScale.kindNameRef());

      executionLogCallback.saveExecutionLog("\nQuerying current replicas");
      Integer currentReplicas = k8sTaskHelper.getCurrentReplicas(client, resourceIdToScale, k8sDelegateTaskParams);
      executionLogCallback.saveExecutionLog("Current replica count is " + currentReplicas);

      switch (k8sScaleTaskParameters.getInstanceUnitType()) {
        case COUNT:
          targetReplicaCount = k8sScaleTaskParameters.getInstances();
          break;

        case PERCENTAGE:
          Integer maxInstances;
          if (k8sScaleTaskParameters.getMaxInstances().isPresent()) {
            maxInstances = k8sScaleTaskParameters.getMaxInstances().get();
          } else {
            maxInstances = currentReplicas;
          }
          targetReplicaCount = (int) Math.round(k8sScaleTaskParameters.getInstances() * maxInstances / 100.0);
          break;

        default:
          unhandled(k8sScaleTaskParameters.getInstanceUnitType());
      }

      executionLogCallback.saveExecutionLog("Target replica count is " + targetReplicaCount);

      executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);

      return true;
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }
}
