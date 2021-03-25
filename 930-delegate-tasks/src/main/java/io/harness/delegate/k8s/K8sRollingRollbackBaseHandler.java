package io.harness.delegate.k8s;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getExecutionLogOutputStream;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getOcCommandPrefix;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.k8s.K8sConstants.ocRolloutUndoCommand;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Cyan;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.delegate.k8s.beans.K8sRollingRollbackHandlerConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
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
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@Singleton
@Slf4j
public class K8sRollingRollbackBaseHandler {
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;

  public boolean init(K8sRollingRollbackHandlerConfig rollbackHandlerConfig, String releaseName,
      LogCallback logCallback) throws IOException {
    String releaseHistoryData =
        k8sTaskHelperBase.getReleaseHistoryData(rollbackHandlerConfig.getKubernetesConfig(), releaseName);

    if (StringUtils.isEmpty(releaseHistoryData)) {
      rollbackHandlerConfig.setNoopRollBack(true);
      logCallback.saveExecutionLog("\nNo release history found for release " + releaseName);
    } else {
      rollbackHandlerConfig.setReleaseHistory(ReleaseHistory.createFromData(releaseHistoryData));
      try {
        rollbackHandlerConfig.setRelease(rollbackHandlerConfig.getReleaseHistory().getLatestRelease());
        printManagedWorkloads(rollbackHandlerConfig, logCallback);
      } catch (Exception e) {
        log.error("Failed to get latest release", e);
      }
    }

    logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  public void steadyStateCheck(K8sRollingRollbackHandlerConfig rollbackHandlerConfig,
      K8sDelegateTaskParams k8sDelegateTaskParams, Integer timeoutInMin, LogCallback logCallback) throws Exception {
    Release release = rollbackHandlerConfig.getRelease();
    Kubectl client = rollbackHandlerConfig.getClient();
    KubernetesConfig kubernetesConfig = rollbackHandlerConfig.getKubernetesConfig();
    List<KubernetesResourceIdRevision> previousManagedWorkloads = rollbackHandlerConfig.getPreviousManagedWorkloads();
    List<KubernetesResource> previousCustomManagedWorkloads = rollbackHandlerConfig.getPreviousCustomManagedWorkloads();
    if (hasNone(previousManagedWorkloads) && hasNone(previousCustomManagedWorkloads)) {
      logCallback.saveExecutionLog(
          "Skipping Status Check since there is no previous eligible Managed Workload.", INFO, SUCCESS);
    } else {
      long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(timeoutInMin);
      List<KubernetesResourceId> kubernetesResourceIds =
          previousManagedWorkloads.stream().map(KubernetesResourceIdRevision::getWorkload).collect(Collectors.toList());
      k8sTaskHelperBase.doStatusCheckForAllResources(client, kubernetesResourceIds, k8sDelegateTaskParams,
          kubernetesConfig.getNamespace(), logCallback, previousCustomManagedWorkloads.isEmpty());

      if (hasSome(previousCustomManagedWorkloads)) {
        k8sTaskHelperBase.checkSteadyStateCondition(previousCustomManagedWorkloads);
        k8sTaskHelperBase.doStatusCheckForAllCustomResources(client, previousCustomManagedWorkloads,
            k8sDelegateTaskParams, logCallback, true, steadyStateTimeoutInMillis);
      }
      release.setStatus(Release.Status.Failed);
      // update the revision on the previous release.
      updateManagedWorkloadRevisionsInRelease(rollbackHandlerConfig, k8sDelegateTaskParams);
    }
  }

  public void postProcess(K8sRollingRollbackHandlerConfig rollbackHandlerConfig, String releaseName) throws Exception {
    boolean isNoopRollBack = rollbackHandlerConfig.isNoopRollBack();
    KubernetesConfig kubernetesConfig = rollbackHandlerConfig.getKubernetesConfig();
    ReleaseHistory releaseHistory = rollbackHandlerConfig.getReleaseHistory();
    List<KubernetesResource> previousCustomManagedWorkloads = rollbackHandlerConfig.getPreviousCustomManagedWorkloads();
    if (!isNoopRollBack) {
      k8sTaskHelperBase.saveReleaseHistory(
          kubernetesConfig, releaseName, releaseHistory.getAsYaml(), !previousCustomManagedWorkloads.isEmpty());
    }
  }

  public boolean rollback(K8sRollingRollbackHandlerConfig rollbackHandlerConfig,
      K8sDelegateTaskParams k8sDelegateTaskParams, Integer releaseNumber, LogCallback logCallback) throws Exception {
    Release release = rollbackHandlerConfig.getRelease();
    ReleaseHistory releaseHistory = rollbackHandlerConfig.getReleaseHistory();
    if (release == null) {
      logCallback.saveExecutionLog("No previous release found. Skipping rollback.");
      logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    if (hasNone(release.getManagedWorkloads()) && hasNone(release.getCustomWorkloads())
        && release.getManagedWorkload() == null) {
      logCallback.saveExecutionLog("\nNo Managed Workload found. Skipping rollback.");
      logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    int rollbackReleaseNumber = releaseNumber != null ? releaseNumber : 0;
    if (rollbackReleaseNumber == 0) { // RollingDeploy was aborted
      if (release.getStatus() == Release.Status.Succeeded) {
        logCallback.saveExecutionLog("No failed release found. Skipping rollback.");
        logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      } else {
        // set releaseNumber to max int so that rollback to current successful one goes through.
        rollbackReleaseNumber = Integer.MAX_VALUE;
      }
    }

    rollbackHandlerConfig.setPreviousRollbackEligibleRelease(
        releaseHistory.getPreviousRollbackEligibleRelease(rollbackReleaseNumber));
    Release previousRollbackEligibleRelease = rollbackHandlerConfig.getPreviousRollbackEligibleRelease();
    if (previousRollbackEligibleRelease == null) {
      logCallback.saveExecutionLog("No previous eligible release found. Can't rollback.");
      logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    logCallback.saveExecutionLog("Previous eligible Release is " + previousRollbackEligibleRelease.getNumber()
        + " with status " + previousRollbackEligibleRelease.getStatus());

    if (hasNone(previousRollbackEligibleRelease.getManagedWorkloads())
        && previousRollbackEligibleRelease.getManagedWorkload() == null
        && hasNone(previousRollbackEligibleRelease.getCustomWorkloads())) {
      logCallback.saveExecutionLog("No Managed Workload found in previous eligible release. Skipping rollback.");
      logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    if (hasSome(previousRollbackEligibleRelease.getCustomWorkloads())) {
      rollbackHandlerConfig.getPreviousCustomManagedWorkloads().addAll(
          previousRollbackEligibleRelease.getCustomWorkloads());
    }

    List<KubernetesResourceIdRevision> previousManagedWorkloads = new ArrayList<>();
    if (hasSome(previousRollbackEligibleRelease.getManagedWorkloads())) {
      previousManagedWorkloads.addAll(previousRollbackEligibleRelease.getManagedWorkloads());
    } else if (previousRollbackEligibleRelease.getManagedWorkload() != null) {
      previousManagedWorkloads.add(KubernetesResourceIdRevision.builder()
                                       .workload(previousRollbackEligibleRelease.getManagedWorkload())
                                       .revision(previousRollbackEligibleRelease.getManagedWorkloadRevision())
                                       .build());
    }
    rollbackHandlerConfig.setPreviousManagedWorkloads(previousManagedWorkloads);

    boolean success = rollback(rollbackHandlerConfig, k8sDelegateTaskParams, logCallback);
    if (!success) {
      logCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  private boolean rollback(K8sRollingRollbackHandlerConfig rollbackHandlerConfig,
      K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback logCallback) throws Exception {
    boolean success = true;
    Release release = rollbackHandlerConfig.getRelease();
    Kubectl client = rollbackHandlerConfig.getClient();
    Release previousRollbackEligibleRelease = rollbackHandlerConfig.getPreviousRollbackEligibleRelease();
    List<KubernetesResource> previousCustomManagedWorkloads = rollbackHandlerConfig.getPreviousCustomManagedWorkloads();
    List<KubernetesResourceIdRevision> previousManagedWorkloads = rollbackHandlerConfig.getPreviousManagedWorkloads();
    if (hasSome(previousCustomManagedWorkloads)) {
      if (hasSome(release.getCustomWorkloads())) {
        logCallback.saveExecutionLog("\nDeleting current custom resources "
            + k8sTaskHelperBase.getResourcesInTableFormat(release.getCustomWorkloads()));

        k8sTaskHelperBase.delete(client, k8sDelegateTaskParams,
            release.getCustomWorkloads().stream().map(KubernetesResource::getResourceId).collect(Collectors.toList()),
            logCallback, false);
      }
      logCallback.saveExecutionLog("\nRolling back custom resource by applying previous release manifests "
          + k8sTaskHelperBase.getResourcesInTableFormat(previousCustomManagedWorkloads));
      success = k8sTaskHelperBase.applyManifests(
          client, previousCustomManagedWorkloads, k8sDelegateTaskParams, logCallback, false);
    }

    logCallback.saveExecutionLog("\nRolling back to release " + previousRollbackEligibleRelease.getNumber());

    for (KubernetesResourceIdRevision kubernetesResourceIdRevision : previousManagedWorkloads) {
      logCallback.saveExecutionLog(format("%nRolling back resource %s in namespace %s to revision %s",
          kubernetesResourceIdRevision.getWorkload().kindNameRef(),
          kubernetesResourceIdRevision.getWorkload().getNamespace(), kubernetesResourceIdRevision.getRevision()));

      ProcessResult result;

      KubernetesResourceId resourceId = kubernetesResourceIdRevision.getWorkload();
      if (Kind.DeploymentConfig.name().equals(resourceId.getKind())) {
        String rolloutUndoCommand = getRolloutUndoCommandForDeploymentConfig(k8sDelegateTaskParams,
            kubernetesResourceIdRevision.getWorkload(), kubernetesResourceIdRevision.getRevision());

        String printableCommand = rolloutUndoCommand.substring(rolloutUndoCommand.indexOf("oc --kubeconfig"));
        logCallback.saveExecutionLog(printableCommand + "\n");

        try (LogOutputStream logOutputStream = getExecutionLogOutputStream(logCallback, INFO);
             LogOutputStream logErrorStream = getExecutionLogOutputStream(logCallback, ERROR);) {
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

        result = runK8sExecutable(k8sDelegateTaskParams, logCallback, rolloutUndoCommand);
      }

      if (result.getExitValue() != 0) {
        logCallback.saveExecutionLog(format("%nFailed to rollback resource %s in namespace %s to revision %s. Error %s",
            kubernetesResourceIdRevision.getWorkload().kindNameRef(),
            kubernetesResourceIdRevision.getWorkload().getNamespace(), kubernetesResourceIdRevision.getRevision(),
            result.getOutput()));

        return false;
      }
    }

    return success;
  }

  private void printManagedWorkloads(K8sRollingRollbackHandlerConfig handlerConfig, LogCallback logCallback) {
    Release release = handlerConfig.getRelease();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();

    if (hasSome(release.getCustomWorkloads())) {
      kubernetesResources.addAll(release.getCustomWorkloads());
    }

    if (hasSome(release.getManagedWorkloads())) {
      for (KubernetesResourceIdRevision kubernetesResourceIdRevision : release.getManagedWorkloads()) {
        kubernetesResources.add(
            KubernetesResource.builder().resourceId(kubernetesResourceIdRevision.getWorkload()).build());
      }
    } else if (release.getManagedWorkload() != null) {
      kubernetesResources.add(KubernetesResource.builder().resourceId(release.getManagedWorkload()).build());
    }

    if (hasSome(kubernetesResources)) {
      logCallback.saveExecutionLog(color("\nFound following Managed Workloads: \n", Cyan, Bold)
          + k8sTaskHelperBase.getResourcesInTableFormat(kubernetesResources));
    }
  }

  private void updateManagedWorkloadRevisionsInRelease(K8sRollingRollbackHandlerConfig rollbackHandlerConfig,
      K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    Release previousRollbackEligibleRelease = rollbackHandlerConfig.getPreviousRollbackEligibleRelease();
    if (hasSome(previousRollbackEligibleRelease.getManagedWorkloads())) {
      for (KubernetesResourceIdRevision kubernetesResourceIdRevision :
          previousRollbackEligibleRelease.getManagedWorkloads()) {
        String latestRevision = k8sTaskHelperBase.getLatestRevision(
            rollbackHandlerConfig.getClient(), kubernetesResourceIdRevision.getWorkload(), k8sDelegateTaskParams);

        kubernetesResourceIdRevision.setRevision(latestRevision);
      }
    } else if (previousRollbackEligibleRelease.getManagedWorkload() != null) {
      previousRollbackEligibleRelease.setManagedWorkloadRevision(
          k8sTaskHelperBase.getLatestRevision(rollbackHandlerConfig.getClient(),
              previousRollbackEligibleRelease.getManagedWorkload(), k8sDelegateTaskParams));
    }
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

  @VisibleForTesting
  ProcessResult executeScript(K8sDelegateTaskParams k8sDelegateTaskParams, String rolloutUndoCommand,
      LogOutputStream logOutputStream, LogOutputStream logErrorStream) throws Exception {
    return Utils.executeScript(
        k8sDelegateTaskParams.getWorkingDirectory(), rolloutUndoCommand, logOutputStream, logErrorStream);
  }

  @VisibleForTesting
  ProcessResult runK8sExecutable(K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback logCallback,
      RolloutUndoCommand rolloutUndoCommand) throws Exception {
    return K8sTaskHelperBase.executeCommand(
        rolloutUndoCommand, k8sDelegateTaskParams.getWorkingDirectory(), logCallback);
  }
}
