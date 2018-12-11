package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.manifest.ManifestHelper.currentReleaseExpression;
import static io.harness.k8s.manifest.ManifestHelper.previousReleaseExpression;
import static io.harness.k8s.model.KubernetesResourceId.createKubernetesResourceIdFromKindName;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Scale;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.delegatetasks.k8s.Utils.doStatusCheck;
import static software.wings.delegatetasks.k8s.Utils.getCurrentReplicas;
import static software.wings.delegatetasks.k8s.Utils.scale;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sScaleTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sScaleResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.utils.Misc;

import java.util.Collections;

@NoArgsConstructor
public class K8sScaleTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sScaleTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private KubernetesResourceId resourceIdToScale;
  private int targetReplicaCount;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sScaleTaskParameters)) {
      throw new InvalidArgumentsException(Pair.of("k8sTaskParameters", "Must be instance of K8sScaleTaskParameters"));
    }

    K8sScaleTaskParameters k8sScaleTaskParameters = (K8sScaleTaskParameters) k8sTaskParameters;

    boolean success = init(k8sScaleTaskParameters, k8sDelegateTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sScaleTaskParameters.getAccountId(),
            k8sScaleTaskParameters.getAppId(), k8sScaleTaskParameters.getActivityId(), Init));

    if (!success) {
      return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    if (resourceIdToScale == null) {
      return K8sTaskExecutionResponse.builder()
          .commandExecutionStatus(SUCCESS)
          .k8sTaskResponse(K8sScaleResponse.builder().build())
          .build();
    }

    success = scale(client, k8sDelegateTaskParams, resourceIdToScale, targetReplicaCount,
        new ExecutionLogCallback(delegateLogService, k8sScaleTaskParameters.getAccountId(),
            k8sScaleTaskParameters.getAppId(), k8sScaleTaskParameters.getActivityId(), Scale));

    if (!success) {
      return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    if (!k8sScaleTaskParameters.isSkipSteadyStateCheck()) {
      success = doStatusCheck(client, resourceIdToScale, k8sDelegateTaskParams,
          new ExecutionLogCallback(delegateLogService, k8sTaskParameters.getAccountId(), k8sTaskParameters.getAppId(),
              k8sTaskParameters.getActivityId(), WaitForSteadyState));

      if (!success) {
        return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
      }
    }

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .k8sTaskResponse(K8sScaleResponse.builder().build())
        .build();
  }

  private boolean init(K8sScaleTaskParameters k8sScaleTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    try {
      kubernetesConfig =
          containerDeploymentDelegateHelper.getKubernetesConfig(k8sScaleTaskParameters.getK8sClusterConfig());

      client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

      if (k8sScaleTaskParameters.getResource().startsWith("${kubernetes.")) {
        ReleaseHistory releaseHistory;
        Release release;

        String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
            kubernetesConfig, Collections.emptyList(), k8sScaleTaskParameters.getReleaseName());

        if (StringUtils.isEmpty(releaseHistoryData)) {
          executionLogCallback.saveExecutionLog(
              "\nNo release history found. Couldn't resolve " + k8sScaleTaskParameters.getResource(), INFO, FAILURE);
          return false;
        } else {
          releaseHistory = ReleaseHistory.createFromData(releaseHistoryData);
          if (k8sScaleTaskParameters.getResource().contains(currentReleaseExpression)) {
            release = releaseHistory.getLatestRelease();
          } else if (k8sScaleTaskParameters.getResource().contains(previousReleaseExpression)) {
            release = releaseHistory.getPreviousRollbackEligibleRelease(releaseHistory.getLatestRelease().getNumber());
            if (release == null) {
              executionLogCallback.saveExecutionLog("\nNo previous release found. Skipping scale.", INFO, SUCCESS);
              return true;
            }
          } else {
            executionLogCallback.saveExecutionLog(
                "\nUnknown expression " + k8sScaleTaskParameters.getResource(), INFO, FAILURE);
            return false;
          }
          resourceIdToScale = release.getManagedWorkload().cloneInternal();
        }
      } else {
        resourceIdToScale = createKubernetesResourceIdFromKindName(k8sScaleTaskParameters.getResource());
      }

      executionLogCallback.saveExecutionLog("\nWorkload to scale is: " + resourceIdToScale.kindNameRef());

      executionLogCallback.saveExecutionLog("\nQuerying current replicas");
      int currentReplicas = getCurrentReplicas(client, resourceIdToScale, k8sDelegateTaskParams);
      executionLogCallback.saveExecutionLog("Current replica count is " + currentReplicas);

      switch (k8sScaleTaskParameters.getInstanceUnitType()) {
        case COUNT:
          targetReplicaCount = k8sScaleTaskParameters.getInstances();
          break;

        case PERCENTAGE:
          int maxInstances;
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
