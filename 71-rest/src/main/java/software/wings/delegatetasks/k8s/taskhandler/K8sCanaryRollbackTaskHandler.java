package software.wings.delegatetasks.k8s.taskhandler;

import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Rollback;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
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
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sCanaryRollbackTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;

import java.io.IOException;
import java.util.Collections;

@NoArgsConstructor
public class K8sCanaryRollbackTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sCanaryRollbackTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  Release release;
  private K8sTaskResponse k8sTaskResponse;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sCanaryRollbackTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8STaskParameters", "Must be instance of K8sCanaryRollbackTaskParameters"));
    }

    K8sCanaryRollbackTaskParameters k8sCanaryRollbackTaskParameters =
        (K8sCanaryRollbackTaskParameters) k8sTaskParameters;

    boolean success = init(k8sCanaryRollbackTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sCanaryRollbackTaskParameters, Init));

    if (!success) {
      return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = rollback(k8sCanaryRollbackTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sCanaryRollbackTaskParameters, Rollback));

    if (!success) {
      return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    release.setStatus(Status.Failed);
    release.setManagedWorkloadRevision(
        k8sTaskHelper.getLatestRevision(client, release.getManagedWorkload(), k8sDelegateTaskParams));

    kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sCanaryRollbackTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

    return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }

  private boolean init(K8sCanaryRollbackTaskParameters request, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    release = releaseHistory.getLatestRelease();

    executionLogCallback.saveExecutionLog("\nManaged Workload is: " + release.getManagedWorkload().kindNameRef());

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  private boolean rollback(K8sCanaryRollbackTaskParameters k8sCanaryRollbackTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    if (k8sCanaryRollbackTaskParameters.getReleaseNumber() == 0) {
      executionLogCallback.saveExecutionLog("No releaseNumber found. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    Release previousRollbackEligibleRelease =
        releaseHistory.getPreviousRollbackEligibleRelease(k8sCanaryRollbackTaskParameters.getReleaseNumber());

    if (previousRollbackEligibleRelease == null) {
      executionLogCallback.saveExecutionLog("No previous eligible release found. Can't rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    executionLogCallback.saveExecutionLog("Previous eligible Release is " + previousRollbackEligibleRelease.getNumber()
        + " with status " + previousRollbackEligibleRelease.getStatus());

    executionLogCallback.saveExecutionLog("\nRolling back to release " + previousRollbackEligibleRelease.getNumber());

    k8sTaskHelper.scale(
        client, k8sDelegateTaskParams, releaseHistory.getLatestRelease().getManagedWorkload(), 0, executionLogCallback);

    k8sTaskHelper.scale(client, k8sDelegateTaskParams, previousRollbackEligibleRelease.getManagedWorkload(),
        k8sCanaryRollbackTaskParameters.getTargetReplicas(), executionLogCallback);

    return true;
  }
}