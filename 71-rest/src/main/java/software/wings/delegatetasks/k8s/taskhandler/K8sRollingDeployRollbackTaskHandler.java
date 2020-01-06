package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.lang.String.format;
import static software.wings.beans.Log.LogColor.Cyan;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Rollback;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutUndoCommand;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.KubernetesResourceIdRevision;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Slf4j
public class K8sRollingDeployRollbackTaskHandler extends K8sTaskHandler {
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private Release release;
  private Release previousRollbackEligibleRelease;
  private List<KubernetesResourceIdRevision> previousManagedWorkloads = new ArrayList<>();

  @Override
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

    if (isEmpty(previousManagedWorkloads)) {
      k8sTaskHelper.getExecutionLogCallback(request, WaitForSteadyState)
          .saveExecutionLog(
              "Skipping Status Check since there is no previous eligible Managed Workload.", INFO, SUCCESS);
    } else {
      List<KubernetesResourceId> kubernetesResourceIds =
          previousManagedWorkloads.stream().map(KubernetesResourceIdRevision::getWorkload).collect(Collectors.toList());
      k8sTaskHelper.doStatusCheckForAllResources(client, kubernetesResourceIds, k8sDelegateTaskParams,
          kubernetesConfig.getNamespace(), k8sTaskHelper.getExecutionLogCallback(request, WaitForSteadyState));

      release.setStatus(Status.Failed);
      // update the revision on the previous release.
      updateManagedWorkloadRevisionsInRelease(k8sDelegateTaskParams);
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
      try {
        printManagedWorkloads(executionLogCallback);
      } catch (Exception e) {
        logger.error("Failed to get latest release", e);
      }
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  public boolean rollback(K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    if (release == null) {
      executionLogCallback.saveExecutionLog("No previous release found. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    if (isEmpty(release.getManagedWorkloads()) && release.getManagedWorkload() == null) {
      executionLogCallback.saveExecutionLog("\nNo Managed Workload found. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    int releaseNumber = k8sRollingDeployRollbackTaskParameters.getReleaseNumber() != null
        ? k8sRollingDeployRollbackTaskParameters.getReleaseNumber()
        : 0;
    if (releaseNumber == 0) { // RollingDeploy was aborted
      if (release.getStatus() == Status.Succeeded) {
        executionLogCallback.saveExecutionLog("No failed release found. Skipping rollback.");
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      } else {
        // set releaseNumber to max int so that rollback to current successful one goes through.
        releaseNumber = Integer.MAX_VALUE;
      }
    }

    previousRollbackEligibleRelease = releaseHistory.getPreviousRollbackEligibleRelease(releaseNumber);
    if (previousRollbackEligibleRelease == null) {
      executionLogCallback.saveExecutionLog("No previous eligible release found. Can't rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    executionLogCallback.saveExecutionLog("Previous eligible Release is " + previousRollbackEligibleRelease.getNumber()
        + " with status " + previousRollbackEligibleRelease.getStatus());

    if (isEmpty(previousRollbackEligibleRelease.getManagedWorkloads())
        && previousRollbackEligibleRelease.getManagedWorkload() == null) {
      executionLogCallback.saveExecutionLog(
          "No Managed Workload found in previous eligible release. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    if (isNotEmpty(previousRollbackEligibleRelease.getManagedWorkloads())) {
      previousManagedWorkloads.addAll(previousRollbackEligibleRelease.getManagedWorkloads());
    } else if (previousRollbackEligibleRelease.getManagedWorkload() != null) {
      previousManagedWorkloads.add(KubernetesResourceIdRevision.builder()
                                       .workload(previousRollbackEligibleRelease.getManagedWorkload())
                                       .revision(previousRollbackEligibleRelease.getManagedWorkloadRevision())
                                       .build());
    }

    boolean success = rollback(k8sDelegateTaskParams, executionLogCallback);
    if (!success) {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  private boolean rollback(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("\nRolling back to release " + previousRollbackEligibleRelease.getNumber());

    for (KubernetesResourceIdRevision kubernetesResourceIdRevision : previousManagedWorkloads) {
      executionLogCallback.saveExecutionLog(format("%nRolling back resource %s in namespace %s to revision %s",
          kubernetesResourceIdRevision.getWorkload().kindNameRef(),
          kubernetesResourceIdRevision.getWorkload().getNamespace(), kubernetesResourceIdRevision.getRevision()));

      RolloutUndoCommand rolloutUndoCommand = client.rollout()
                                                  .undo()
                                                  .resource(kubernetesResourceIdRevision.getWorkload().kindNameRef())
                                                  .namespace(kubernetesResourceIdRevision.getWorkload().getNamespace())
                                                  .toRevision(kubernetesResourceIdRevision.getRevision());

      ProcessResult result = K8sTaskHelper.executeCommand(
          rolloutUndoCommand, k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);

      if (result.getExitValue() != 0) {
        executionLogCallback.saveExecutionLog(
            format("%nFailed to rollback resource %s in namespace %s to revision %s. Error %s",
                kubernetesResourceIdRevision.getWorkload().kindNameRef(),
                kubernetesResourceIdRevision.getWorkload().getNamespace(), kubernetesResourceIdRevision.getRevision(),
                result.getOutput()));

        return false;
      }
    }

    return true;
  }

  private void printManagedWorkloads(ExecutionLogCallback executionLogCallback) {
    release = releaseHistory.getLatestRelease();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();

    if (isNotEmpty(release.getManagedWorkloads())) {
      for (KubernetesResourceIdRevision kubernetesResourceIdRevision : release.getManagedWorkloads()) {
        kubernetesResources.add(
            KubernetesResource.builder().resourceId(kubernetesResourceIdRevision.getWorkload()).build());
      }
    } else if (release.getManagedWorkload() != null) {
      kubernetesResources.add(KubernetesResource.builder().resourceId(release.getManagedWorkload()).build());
    }

    if (isNotEmpty(kubernetesResources)) {
      executionLogCallback.saveExecutionLog(color("\nFound following Managed Workloads: \n", Cyan, Bold)
          + k8sTaskHelper.getResourcesInTableFormat(kubernetesResources));
    }
  }

  private void updateManagedWorkloadRevisionsInRelease(K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (isNotEmpty(previousRollbackEligibleRelease.getManagedWorkloads())) {
      for (KubernetesResourceIdRevision kubernetesResourceIdRevision :
          previousRollbackEligibleRelease.getManagedWorkloads()) {
        String latestRevision =
            k8sTaskHelper.getLatestRevision(client, kubernetesResourceIdRevision.getWorkload(), k8sDelegateTaskParams);

        kubernetesResourceIdRevision.setRevision(latestRevision);
      }
    } else if (previousRollbackEligibleRelease.getManagedWorkload() != null) {
      previousRollbackEligibleRelease.setManagedWorkloadRevision(k8sTaskHelper.getLatestRevision(
          client, previousRollbackEligibleRelease.getManagedWorkload(), k8sDelegateTaskParams));
    }
  }
}
