package software.wings.delegatetasks.k8s.taskhandler;

import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Rollback;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutUndoCommand;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.IOException;
import java.util.Collections;

@NoArgsConstructor
public class K8sRollingDeployRollbackTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sRollingDeployRollbackTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private Release release;
  private Release previousRollbackEligibleRelease;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sRollingDeployRollbackTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sRollingDeployRollbackTaskParameters"));
    }

    K8sRollingDeployRollbackTaskParameters request = (K8sRollingDeployRollbackTaskParameters) k8sTaskParameters;

    boolean success = init(request, k8sDelegateTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), Init));

    if (!success) {
      return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = rollback(request, k8sDelegateTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), Rollback));

    if (!success) {
      return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    if (release.getManagedWorkload() == null) {
      k8sTaskHelper.getExecutionLogCallback(request, WaitForSteadyState)
          .saveExecutionLog("Skipping Status Check since there is no Managed Workload.", INFO, SUCCESS);
    } else {
      k8sTaskHelper.doStatusCheck(client, release.getManagedWorkload(), k8sDelegateTaskParams,
          k8sTaskHelper.getExecutionLogCallback(request, WaitForSteadyState));

      release.setStatus(Status.Failed);
      // update the revision on the previous release.
      previousRollbackEligibleRelease.setManagedWorkloadRevision(
          k8sTaskHelper.getLatestRevision(client, release.getManagedWorkload(), k8sDelegateTaskParams));
    }

    kubernetesContainerService.saveReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName(), releaseHistory.getAsYaml());

    return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }

  private boolean init(K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(
        k8sRollingDeployRollbackTaskParameters.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), k8sRollingDeployRollbackTaskParameters.getReleaseName());

    if (StringUtils.isEmpty(releaseHistoryData)) {
      executionLogCallback.saveExecutionLog(
          "\nNo release history found for release " + k8sRollingDeployRollbackTaskParameters.getReleaseName());
    } else {
      releaseHistory = ReleaseHistory.createFromData(releaseHistoryData);
      release = releaseHistory.getLatestRelease();
      if (release.getManagedWorkload() != null) {
        executionLogCallback.saveExecutionLog("\nManaged Workload is: " + release.getManagedWorkload().kindNameRef());
      }
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  public boolean rollback(K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    if (k8sRollingDeployRollbackTaskParameters.getReleaseNumber() == 0) {
      executionLogCallback.saveExecutionLog("No releaseNumber found. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    if (release.getManagedWorkload() == null) {
      executionLogCallback.saveExecutionLog("\nNo Managed Workload found. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    previousRollbackEligibleRelease =
        releaseHistory.getPreviousRollbackEligibleRelease(k8sRollingDeployRollbackTaskParameters.getReleaseNumber());

    if (previousRollbackEligibleRelease == null) {
      executionLogCallback.saveExecutionLog("No previous eligible release found. Can't rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    executionLogCallback.saveExecutionLog("Previous eligible Release is " + previousRollbackEligibleRelease.getNumber()
        + " with status " + previousRollbackEligibleRelease.getStatus());

    if (previousRollbackEligibleRelease.getManagedWorkload() == null) {
      executionLogCallback.saveExecutionLog(
          "No Managed Workload found in previous eligible release. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    executionLogCallback.saveExecutionLog("\nRolling back to release " + previousRollbackEligibleRelease.getNumber());

    RolloutUndoCommand rolloutUndoCommand =
        client.rollout()
            .undo()
            .resource(previousRollbackEligibleRelease.getManagedWorkload().kindNameRef())
            .namespace(previousRollbackEligibleRelease.getManagedWorkload().getNamespace())
            .toRevision(previousRollbackEligibleRelease.getManagedWorkloadRevision());

    ProcessResult result = K8sTaskHelper.executeCommand(
        rolloutUndoCommand, k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);

    if (result.getExitValue() == 0) {
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    } else {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      logger.warn("Failed to rollback resource. Error {}", result.getOutput());
      return false;
    }
  }
}
