package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.manifest.ManifestHelper.getManagedWorkload;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Prepare;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.beans.command.K8sDummyCommandUnit.WrapUp;
import static software.wings.delegatetasks.k8s.Utils.applyManifests;
import static software.wings.delegatetasks.k8s.Utils.doStatusCheck;
import static software.wings.delegatetasks.k8s.Utils.getLatestRevision;
import static software.wings.delegatetasks.k8s.Utils.getResourcesInTableFormat;
import static software.wings.delegatetasks.k8s.Utils.readManifests;
import static software.wings.delegatetasks.k8s.Utils.renderTemplate;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.DeleteCommand;
import io.harness.k8s.kubectl.DescribeCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
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
import software.wings.delegatetasks.k8s.K8sCommandTaskParams;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sDeploymentRollingSetupRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sDeploymentRollingSetupResponse;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Singleton
public class K8sDeploymentRollingCommandTaskHandler extends K8sCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sDeploymentRollingCommandTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  Release release;
  KubernetesResourceId managedWorkload;
  List<KubernetesResource> resources;

  public K8sCommandExecutionResponse executeTaskInternal(
      K8sCommandRequest k8sCommandRequest, K8sCommandTaskParams k8sCommandTaskParams) throws Exception {
    if (!(k8sCommandRequest instanceof K8sDeploymentRollingSetupRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sCommandRequest", "Must be instance of K8sDeploymentRollingSetupRequest"));
    }

    K8sDeploymentRollingSetupRequest request = (K8sDeploymentRollingSetupRequest) k8sCommandRequest;

    boolean success = init(request, k8sCommandTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), Init));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = prepareForRolling(k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), Prepare));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = applyManifests(client, resources, k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), Apply));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    release.setManagedWorkload(managedWorkload);
    release.setManagedWorkloadRevision(getLatestRevision(client, managedWorkload, k8sCommandTaskParams));

    success = doStatusCheck(client, managedWorkload, k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), WaitForSteadyState));

    if (!success) {
      releaseHistory.setReleaseStatus(Status.Failed);
      kubernetesContainerService.saveReleaseHistory(
          kubernetesConfig, Collections.emptyList(), request.getReleaseName(), releaseHistory.getAsYaml());
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    wrapUp(k8sCommandTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), WrapUp));

    releaseHistory.setReleaseStatus(Status.Succeeded);
    kubernetesContainerService.saveReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName(), releaseHistory.getAsYaml());

    return K8sCommandExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sCommandResponse(K8sDeploymentRollingSetupResponse.builder().releaseNumber(release.getNumber()).build())
        .build();
  }

  private boolean init(K8sDeploymentRollingSetupRequest request, K8sCommandTaskParams k8sCommandTaskParams,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig());

    client = Kubectl.client(k8sCommandTaskParams.getKubectlPath(), k8sCommandTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    try {
      List<ManifestFile> manifestFiles =
          renderTemplate(k8sCommandTaskParams, request.getManifestFiles(), executionLogCallback);
      resources = readManifests(manifestFiles, executionLogCallback);
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(e.getMessage(), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }

    markVersionedResources(resources);

    executionLogCallback.saveExecutionLog(
        "Manifests processed. Found following resources: \n" + getResourcesInTableFormat(resources));

    managedWorkload = getManagedWorkload(resources);

    executionLogCallback.saveExecutionLog("\nManaged Workload is: " + managedWorkload.kindNameRef());

    release = releaseHistory.createNewRelease(
        resources.stream().map(resource -> resource.getResourceId()).collect(Collectors.toList()));

    executionLogCallback.saveExecutionLog("\nCurrent release number is: " + release.getNumber());

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  private boolean prepareForRolling(
      K8sCommandTaskParams k8sCommandTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      executionLogCallback.saveExecutionLog("\nManifests:\n---------\n");

      executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources) + "\n");

      Release lastSuccessfulRelease = releaseHistory.getLastSuccessfulRelease();

      if (lastSuccessfulRelease == null) {
        executionLogCallback.saveExecutionLog("No successful release found. Nothing to cleanup");
      } else {
        executionLogCallback.saveExecutionLog("Last Successful Release is " + lastSuccessfulRelease.getNumber());

        executionLogCallback.saveExecutionLog("\nCleaning up older releases");

        for (int releaseIndex = releaseHistory.getReleases().size() - 1; releaseIndex >= 0; releaseIndex--) {
          Release release = releaseHistory.getReleases().get(releaseIndex);
          if (release.getNumber() < lastSuccessfulRelease.getNumber()) {
            for (int resourceIndex = release.getResources().size() - 1; resourceIndex >= 0; resourceIndex--) {
              KubernetesResourceId resourceId = release.getResources().get(resourceIndex);
              if (resourceId.isVersioned()) {
                DeleteCommand deleteCommand =
                    client.delete().resources(resourceId.kindNameRef()).namespace(resourceId.getNamespace());

                executionLogCallback.saveExecutionLog("\n" + deleteCommand.command());

                ProcessResult result = deleteCommand.execute(k8sCommandTaskParams.getWorkingDirectory(),
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
                    });

                if (result.getExitValue() != 0) {
                  logger.warn("Failed to delete resource {}. Error {}", resourceId.kindNameRef(), result.getOutput());
                }
              }
            }
          } else {
            break;
          }
        }

        releaseHistory.getReleases().removeIf(release -> release.getNumber() < lastSuccessfulRelease.getNumber());
      }

      executionLogCallback.saveExecutionLog("Versioning resources.");

      addRevisionNumber(resources, release.getNumber());

    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  private void wrapUp(K8sCommandTaskParams k8sCommandTaskParams, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    DescribeCommand describeCommand = client.describe().filename("manifests.yaml");

    executionLogCallback.saveExecutionLog(describeCommand.command() + "\n");

    describeCommand.execute(k8sCommandTaskParams.getWorkingDirectory(),
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
        });
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }
}
