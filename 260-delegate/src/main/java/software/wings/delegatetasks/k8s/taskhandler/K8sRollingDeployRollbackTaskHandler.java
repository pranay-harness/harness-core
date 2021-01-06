package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getExecutionLogOutputStream;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getOcCommandPrefix;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Rollback;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.k8s.K8sConstants.ocRolloutUndoCommand;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Cyan;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutUndoCommand;
import io.harness.k8s.kubectl.Utils;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.KubernetesResourceIdRevision;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@NoArgsConstructor
@Slf4j
public class K8sRollingDeployRollbackTaskHandler extends K8sTaskHandler {
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;
  @Inject private transient K8sTaskHelperBase k8sTaskHelperBase;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private Release release;
  private Release previousRollbackEligibleRelease;
  private boolean isNoopRollBack;
  private List<KubernetesResourceIdRevision> previousManagedWorkloads = new ArrayList<>();
  private List<KubernetesResource> previousCustomManagedWorkloads = new ArrayList<>();

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

    if (isEmpty(previousManagedWorkloads) && isEmpty(previousCustomManagedWorkloads)) {
      k8sTaskHelper.getExecutionLogCallback(request, WaitForSteadyState)
          .saveExecutionLog(
              "Skipping Status Check since there is no previous eligible Managed Workload.", INFO, SUCCESS);
    } else {
      long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());
      List<KubernetesResourceId> kubernetesResourceIds =
          previousManagedWorkloads.stream().map(KubernetesResourceIdRevision::getWorkload).collect(Collectors.toList());
      k8sTaskHelperBase.doStatusCheckForAllResources(client, kubernetesResourceIds, k8sDelegateTaskParams,
          kubernetesConfig.getNamespace(), k8sTaskHelper.getExecutionLogCallback(request, WaitForSteadyState),
          previousCustomManagedWorkloads.isEmpty());

      if (isNotEmpty(previousCustomManagedWorkloads)) {
        k8sTaskHelperBase.checkSteadyStateCondition(previousCustomManagedWorkloads);
        k8sTaskHelperBase.doStatusCheckForAllCustomResources(client, previousCustomManagedWorkloads,
            k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(request, WaitForSteadyState), true,
            steadyStateTimeoutInMillis);
      }
      release.setStatus(Status.Failed);
      // update the revision on the previous release.
      updateManagedWorkloadRevisionsInRelease(k8sDelegateTaskParams);
    }

    if (!isNoopRollBack) {
      k8sTaskHelperBase.saveReleaseHistory(kubernetesConfig, request.getReleaseName(), releaseHistory.getAsYaml(),
          !previousCustomManagedWorkloads.isEmpty());
    }

    return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }

  private boolean init(K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(
        k8sRollingDeployRollbackTaskParameters.getK8sClusterConfig(), false);

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = k8sTaskHelperBase.getReleaseHistoryData(
        kubernetesConfig, k8sRollingDeployRollbackTaskParameters.getReleaseName());

    if (StringUtils.isEmpty(releaseHistoryData)) {
      isNoopRollBack = true;
      executionLogCallback.saveExecutionLog(
          "\nNo release history found for release " + k8sRollingDeployRollbackTaskParameters.getReleaseName());
    } else {
      releaseHistory = ReleaseHistory.createFromData(releaseHistoryData);
      try {
        printManagedWorkloads(executionLogCallback);
      } catch (Exception e) {
        log.error("Failed to get latest release", e);
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

    if (isEmpty(release.getManagedWorkloads()) && isEmpty(release.getCustomWorkloads())
        && release.getManagedWorkload() == null) {
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
        && previousRollbackEligibleRelease.getManagedWorkload() == null
        && isEmpty(previousRollbackEligibleRelease.getCustomWorkloads())) {
      executionLogCallback.saveExecutionLog(
          "No Managed Workload found in previous eligible release. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    if (isNotEmpty(previousRollbackEligibleRelease.getCustomWorkloads())) {
      previousCustomManagedWorkloads.addAll(previousRollbackEligibleRelease.getCustomWorkloads());
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
    boolean success = true;
    if (isNotEmpty(previousCustomManagedWorkloads)) {
      if (isNotEmpty(release.getCustomWorkloads())) {
        executionLogCallback.saveExecutionLog("\nDeleting current custom resources "
            + k8sTaskHelperBase.getResourcesInTableFormat(release.getCustomWorkloads()));

        k8sTaskHelperBase.delete(client, k8sDelegateTaskParams,
            release.getCustomWorkloads().stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()),
            executionLogCallback, false);
      }
      executionLogCallback.saveExecutionLog("\nRolling back custom resource by applying previous release manifests "
          + k8sTaskHelperBase.getResourcesInTableFormat(previousCustomManagedWorkloads));
      success = k8sTaskHelperBase.applyManifests(
          client, previousCustomManagedWorkloads, k8sDelegateTaskParams, executionLogCallback, false);
    }

    executionLogCallback.saveExecutionLog("\nRolling back to release " + previousRollbackEligibleRelease.getNumber());

    for (KubernetesResourceIdRevision kubernetesResourceIdRevision : previousManagedWorkloads) {
      executionLogCallback.saveExecutionLog(format("%nRolling back resource %s in namespace %s to revision %s",
          kubernetesResourceIdRevision.getWorkload().kindNameRef(),
          kubernetesResourceIdRevision.getWorkload().getNamespace(), kubernetesResourceIdRevision.getRevision()));

      ProcessResult result;

      KubernetesResourceId resourceId = kubernetesResourceIdRevision.getWorkload();
      if (Kind.DeploymentConfig.name().equals(resourceId.getKind())) {
        String rolloutUndoCommand = getRolloutUndoCommandForDeploymentConfig(k8sDelegateTaskParams,
            kubernetesResourceIdRevision.getWorkload(), kubernetesResourceIdRevision.getRevision());

        String printableCommand = rolloutUndoCommand.substring(rolloutUndoCommand.indexOf("oc --kubeconfig"));
        executionLogCallback.saveExecutionLog(printableCommand + "\n");

        try (LogOutputStream logOutputStream = getExecutionLogOutputStream(executionLogCallback, INFO);
             LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR);) {
          printableCommand = new StringBuilder().append("\n").append(printableCommand).append("\n\n").toString();
          logOutputStream.write(printableCommand.getBytes(StandardCharsets.UTF_8));
          result = executeScript(k8sDelegateTaskParams, rolloutUndoCommand, logOutputStream, logErrorStream);
        }
      } else {
        RolloutUndoCommand rolloutUndoCommand =
            client.rollout()
                .undo()
                .resource(kubernetesResourceIdRevision.getWorkload().kindNameRef())
                .namespace(kubernetesResourceIdRevision.getWorkload().getNamespace())
                .toRevision(kubernetesResourceIdRevision.getRevision());

        result = runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, rolloutUndoCommand);
      }

      if (result.getExitValue() != 0) {
        executionLogCallback.saveExecutionLog(
            format("%nFailed to rollback resource %s in namespace %s to revision %s. Error %s",
                kubernetesResourceIdRevision.getWorkload().kindNameRef(),
                kubernetesResourceIdRevision.getWorkload().getNamespace(), kubernetesResourceIdRevision.getRevision(),
                result.getOutput()));

        return false;
      }
    }

    return success;
  }

  @VisibleForTesting
  ProcessResult executeScript(K8sDelegateTaskParams k8sDelegateTaskParams, String rolloutUndoCommand,
      LogOutputStream logOutputStream, LogOutputStream logErrorStream) throws Exception {
    return Utils.executeScript(
        k8sDelegateTaskParams.getWorkingDirectory(), rolloutUndoCommand, logOutputStream, logErrorStream);
  }

  @VisibleForTesting
  ProcessResult runK8sExecutable(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback,
      RolloutUndoCommand rolloutUndoCommand) throws Exception {
    return K8sTaskHelperBase.executeCommand(
        rolloutUndoCommand, k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);
  }

  private String getRolloutUndoCommandForDeploymentConfig(
      K8sDelegateTaskParams k8sDelegateTaskParams, KubernetesResourceId resourceId, String revision) {
    String namespace = "";
    if (StringUtils.isNotBlank(resourceId.getNamespace())) {
      namespace = "--namespace=" + resourceId.getNamespace() + " ";
    }

    String evaluatedRevision = "";
    if (StringUtils.isNotBlank(revision)) {
      evaluatedRevision = "--to-revision=" + revision;
    }

    return ocRolloutUndoCommand.replace("{OC_COMMAND_PREFIX}", getOcCommandPrefix(k8sDelegateTaskParams))
        .replace("{RESOURCE_ID}", resourceId.kindNameRef())
        .replace("{NAMESPACE}", namespace)
        .replace("{REVISION}", evaluatedRevision)
        .trim();
  }

  private void printManagedWorkloads(ExecutionLogCallback executionLogCallback) {
    release = releaseHistory.getLatestRelease();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();

    if (isNotEmpty(release.getCustomWorkloads())) {
      kubernetesResources.addAll(release.getCustomWorkloads());
    }

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
          + k8sTaskHelperBase.getResourcesInTableFormat(kubernetesResources));
    }
  }

  private void updateManagedWorkloadRevisionsInRelease(K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (isNotEmpty(previousRollbackEligibleRelease.getManagedWorkloads())) {
      for (KubernetesResourceIdRevision kubernetesResourceIdRevision :
          previousRollbackEligibleRelease.getManagedWorkloads()) {
        String latestRevision = k8sTaskHelperBase.getLatestRevision(
            client, kubernetesResourceIdRevision.getWorkload(), k8sDelegateTaskParams);

        kubernetesResourceIdRevision.setRevision(latestRevision);
      }
    } else if (previousRollbackEligibleRelease.getManagedWorkload() != null) {
      previousRollbackEligibleRelease.setManagedWorkloadRevision(k8sTaskHelperBase.getLatestRevision(
          client, previousRollbackEligibleRelease.getManagedWorkload(), k8sDelegateTaskParams));
    }
  }
}
