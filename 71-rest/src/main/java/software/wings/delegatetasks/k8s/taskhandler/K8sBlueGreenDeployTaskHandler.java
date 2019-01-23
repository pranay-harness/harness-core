package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloads;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static io.harness.k8s.model.Kind.Service;
import static software.wings.beans.Log.LogColor.Cyan;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Prepare;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.beans.command.K8sDummyCommandUnit.WrapUp;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.DeleteCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sBlueGreenDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor
public class K8sBlueGreenDeployTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sBlueGreenDeployTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private Release currentRelease;
  private KubernetesResource managedWorkload;
  private List<KubernetesResource> resources;
  int primaryRevision;
  int stageRevision;
  private K8sBlueGreenDeployResponse k8sBlueGreenDeployResponse;
  private String releaseName;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sBlueGreenDeployTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sBlueGreenDeployTaskParameters"));
    }

    K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters =
        (K8sBlueGreenDeployTaskParameters) k8sTaskParameters;

    releaseName = k8sBlueGreenDeployTaskParameters.getReleaseName();

    k8sBlueGreenDeployResponse = K8sBlueGreenDeployResponse.builder().build();

    List<ManifestFile> manifestFiles =
        k8sTaskHelper.fetchManifestFiles(k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig(),
            k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, FetchFiles), gitService,
            encryptionService);
    if (manifestFiles == null) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sBlueGreenDeployResponse, FAILURE);
    }
    k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig().setManifestFiles(manifestFiles);

    boolean success = init(k8sBlueGreenDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Init));
    if (!success) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sBlueGreenDeployResponse, FAILURE);
    }

    success = prepareForBlueGreen(k8sBlueGreenDeployTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Prepare));
    if (!success) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sBlueGreenDeployResponse, FAILURE);
    }

    currentRelease.setManagedWorkload(managedWorkload.getResourceId().cloneInternal());

    success = k8sTaskHelper.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, Apply));
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sBlueGreenDeployResponse, FAILURE);
    }

    currentRelease.setManagedWorkloadRevision(
        k8sTaskHelper.getLatestRevision(client, managedWorkload.getResourceId(), k8sDelegateTaskParams));

    success = k8sTaskHelper.doStatusCheck(client, managedWorkload.getResourceId(), k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, WaitForSteadyState));
    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
          k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sBlueGreenDeployResponse, FAILURE);
    }

    wrapUp(k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(k8sBlueGreenDeployTaskParameters, WrapUp));

    List<K8sPod> podList = k8sTaskHelper.getPodDetailsWithRevision(
        kubernetesConfig, managedWorkload.getResourceId().getNamespace(), releaseName, currentRelease.getNumber());

    releaseHistory.setReleaseStatus(Status.Succeeded);
    kubernetesContainerService.saveReleaseHistory(kubernetesConfig, Collections.emptyList(),
        k8sBlueGreenDeployTaskParameters.getReleaseName(), releaseHistory.getAsYaml());

    k8sBlueGreenDeployResponse.setReleaseNumber(currentRelease.getNumber());
    k8sBlueGreenDeployResponse.setK8sPodList(podList);
    return k8sTaskHelper.getK8sTaskExecutionResponse(k8sBlueGreenDeployResponse, SUCCESS);
  }

  private boolean init(K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sBlueGreenDeployTaskParameters.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), k8sBlueGreenDeployTaskParameters.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      List<ManifestFile> manifestFiles = k8sTaskHelper.renderTemplate(k8sDelegateTaskParams,
          k8sBlueGreenDeployTaskParameters.getK8sDelegateManifestConfig().getManifestFiles(),
          k8sBlueGreenDeployTaskParameters.getValuesYamlList(), executionLogCallback);

      resources = k8sTaskHelper.readManifests(manifestFiles, executionLogCallback);
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));

    executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

    return k8sTaskHelper.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
  }

  private boolean matchServiceName(KubernetesResourceId resourceId, String serviceName) {
    return StringUtils.equals(resourceId.getKind(), Service.name())
        && StringUtils.equals(resourceId.getName(), serviceName);
  }

  private boolean prepareForBlueGreen(K8sBlueGreenDeployTaskParameters k8sBlueGreenDeployTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      List<KubernetesResource> workloads = getWorkloads(resources);

      if (workloads.size() != 1) {
        if (workloads.isEmpty()) {
          executionLogCallback.saveExecutionLog(
              "\nNo workload found in the Manifests. Can't do Blue/Green Deployment.", ERROR, FAILURE);
        } else {
          executionLogCallback.saveExecutionLog(
              "\nMore than one workloads found in the Manifests. Only one can be managed. Others should be marked with annotation "
                  + HarnessAnnotations.directApply + ": true",
              ERROR, FAILURE);
        }
        return false;
      }

      markVersionedResources(resources, true);

      executionLogCallback.saveExecutionLog(
          "Manifests processed. Found following resources: \n" + k8sTaskHelper.getResourcesInTableFormat(resources));

      Optional<KubernetesResource> primaryService =
          resources.stream()
              .filter(resource
                  -> matchServiceName(
                      resource.getResourceId(), k8sBlueGreenDeployTaskParameters.getPrimaryServiceName()))
              .findFirst();

      if (!primaryService.isPresent()) {
        executionLogCallback.saveExecutionLog("Primary Service ["
                + k8sBlueGreenDeployTaskParameters.getPrimaryServiceName() + "] not found in manifests. Failing.",
            ERROR, FAILURE);
        return false;
      }

      Optional<KubernetesResource> stageService =
          resources.stream()
              .filter(resource
                  -> matchServiceName(resource.getResourceId(), k8sBlueGreenDeployTaskParameters.getStageServiceName()))
              .findFirst();
      if (!stageService.isPresent()) {
        executionLogCallback.saveExecutionLog("Stage Service [" + k8sBlueGreenDeployTaskParameters.getStageServiceName()
                + "] not found in manifests. Failing.",
            ERROR, FAILURE);
        return false;
      }

      Service primaryServiceInCluster;
      Service stageServiceInCluster;

      try {
        primaryServiceInCluster = kubernetesContainerService.getService(
            kubernetesConfig, Collections.emptyList(), k8sBlueGreenDeployTaskParameters.getPrimaryServiceName());
        if (primaryServiceInCluster == null) {
          executionLogCallback.saveExecutionLog("Primary Service ["
              + k8sBlueGreenDeployTaskParameters.getPrimaryServiceName() + "] not found in cluster.");
        }

        stageServiceInCluster = kubernetesContainerService.getService(
            kubernetesConfig, Collections.emptyList(), k8sBlueGreenDeployTaskParameters.getStageServiceName());
        if (stageServiceInCluster == null) {
          executionLogCallback.saveExecutionLog(
              "Stage Service [" + k8sBlueGreenDeployTaskParameters.getStageServiceName() + "] not found in cluster.");
        }

        primaryRevision = getRevisionFromService(primaryServiceInCluster);
        stageRevision = getRevisionFromService(stageServiceInCluster);

      } catch (Exception e) {
        executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, FAILURE);
        return false;
      }

      currentRelease = releaseHistory.createNewRelease(
          resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

      cleanupForBlueGreen(k8sDelegateTaskParams, releaseHistory, executionLogCallback);

      executionLogCallback.saveExecutionLog("\nCurrent release number is: " + currentRelease.getNumber());

      executionLogCallback.saveExecutionLog("\nVersioning resources.");

      addRevisionNumber(resources, currentRelease.getNumber(), true);
      managedWorkload = getManagedWorkload(resources);
      managedWorkload.addReleaseLabelsInPodSpec(releaseName, currentRelease.getNumber());
      managedWorkload.addRevisionNumberInDeploymentSelector(currentRelease.getNumber(), false);

      if (currentRelease.getNumber() == 1) {
        primaryService.get().addRevisionSelectorInService(currentRelease.getNumber());
        executionLogCallback.saveExecutionLog("Setting Primary Service ["
            + color(primaryService.get().getResourceId().getName(), Cyan, Bold) + "] at revision "
            + currentRelease.getNumber());
      } else {
        primaryService.get().addRevisionSelectorInService(primaryRevision);
        executionLogCallback.saveExecutionLog("Primary Service ["
            + color(primaryService.get().getResourceId().getName(), Cyan, Bold) + "] remains at revision "
            + primaryRevision);
      }

      stageService.get().addRevisionSelectorInService(currentRelease.getNumber());
      executionLogCallback.saveExecutionLog("Setting Stage Service ["
          + color(stageService.get().getResourceId().getName(), Cyan, Bold) + "] at revision "
          + currentRelease.getNumber());

      executionLogCallback.saveExecutionLog(
          "\nManaged Workload is: " + color(managedWorkload.getResourceId().kindNameRef(), Cyan, Bold));

    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, FAILURE);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  public void cleanupForBlueGreen(K8sDelegateTaskParams k8sDelegateTaskParams, ReleaseHistory releaseHistory,
      ExecutionLogCallback executionLogCallback) throws Exception {
    if (primaryRevision == 0) {
      return;
    }

    executionLogCallback.saveExecutionLog("Primary Service is at revision: " + primaryRevision);
    executionLogCallback.saveExecutionLog("Stage Service is at revision: " + stageRevision);

    executionLogCallback.saveExecutionLog("\nCleaning up non primary releases");

    for (int releaseIndex = releaseHistory.getReleases().size() - 1; releaseIndex >= 0; releaseIndex--) {
      Release release = releaseHistory.getReleases().get(releaseIndex);
      if (release.getNumber() != primaryRevision && release.getNumber() != currentRelease.getNumber()) {
        for (int resourceIndex = release.getResources().size() - 1; resourceIndex >= 0; resourceIndex--) {
          KubernetesResourceId resourceId = release.getResources().get(resourceIndex);
          if (resourceId.isVersioned()) {
            DeleteCommand deleteCommand =
                client.delete().resources(resourceId.kindNameRef()).namespace(resourceId.getNamespace());

            ProcessResult result = deleteCommand.execute(k8sDelegateTaskParams.getWorkingDirectory(),
                new LogOutputStream() {
                  @Override
                  protected void processLine(String line) {
                    executionLogCallback.saveExecutionLog(line, INFO);
                  }
                },
                new LogOutputStream() {
                  @Override
                  protected void processLine(String line) {
                    executionLogCallback.saveExecutionLog(line, ERROR);
                  }
                },
                true);

            if (result.getExitValue() != 0) {
              logger.warn("Failed to delete resource {}. Error {}", resourceId.kindNameRef(), result.getOutput());
            }
          }
        }
      }
    }
    releaseHistory.getReleases().removeIf(
        release -> release.getNumber() != primaryRevision && currentRelease.getNumber() != release.getNumber());
  }

  private int getRevisionFromService(Service service) {
    if (service == null) {
      return 0;
    }
    String revision = service.getSpec().getSelector().get(HarnessLabels.revision);
    if (StringUtils.isEmpty(revision)) {
      return 0;
    }
    return Integer.parseInt(revision);
  }

  private void wrapUp(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    k8sTaskHelper.describe(client, k8sDelegateTaskParams, executionLogCallback);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }
}
