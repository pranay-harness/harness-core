package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.k8s.manifest.ManifestHelper.getWorkloadsForApplyState;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.LogColor.Gray;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogColor.Yellow;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.K8sDummyCommandUnit.Apply;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Prepare;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.beans.command.K8sDummyCommandUnit.WrapUp;
import static software.wings.delegatetasks.k8s.K8sTask.MANIFEST_FILES_DIR;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sApplyResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Slf4j
public class K8sApplyTaskHandler extends K8sTaskHandler {
  @Inject private K8sTaskHelper k8sTaskHelper;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  private Kubectl client;
  private String releaseName;
  private List<KubernetesResource> resources;
  private List<KubernetesResource> workloads;
  private KubernetesConfig kubernetesConfig;
  private String manifestFilesDirectory;

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sApplyTaskParameters)) {
      throw new InvalidArgumentsException(Pair.of("k8sTaskParameters", "Must be instance of K8sApplyTaskParameters"));
    }

    K8sApplyTaskParameters k8sApplyTaskParameters = (K8sApplyTaskParameters) k8sTaskParameters;
    releaseName = k8sApplyTaskParameters.getReleaseName();
    manifestFilesDirectory = Paths.get(k8sDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString();

    K8sApplyResponse k8sApplyResponse = K8sApplyResponse.builder().build();

    boolean success =
        k8sTaskHelper.fetchManifestFilesAndWriteToDirectory(k8sApplyTaskParameters.getK8sDelegateManifestConfig(),
            manifestFilesDirectory, k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, FetchFiles));
    if (!success) {
      return getFailureResponse();
    }

    success = init(k8sApplyTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepare(k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, Prepare));
    if (!success) {
      return getFailureResponse();
    }

    success = k8sTaskHelper.applyManifests(
        client, resources, k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, Apply));
    if (!success) {
      return getFailureResponse();
    }

    if (isEmpty(workloads)) {
      k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, WaitForSteadyState)
          .saveExecutionLog("Skipping Status Check since there is no Workload.", INFO, SUCCESS);
    } else {
      if (!k8sApplyTaskParameters.isSkipSteadyStateCheck()) {
        List<KubernetesResourceId> kubernetesResourceIds =
            workloads.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList());

        success = k8sTaskHelper.doStatusCheckForAllResources(client, kubernetesResourceIds, k8sDelegateTaskParams,
            k8sTaskParameters.getK8sClusterConfig().getNamespace(),
            new ExecutionLogCallback(delegateLogService, k8sTaskParameters.getAccountId(), k8sTaskParameters.getAppId(),
                k8sTaskParameters.getActivityId(), WaitForSteadyState));

        if (!success) {
          return k8sTaskHelper.getK8sTaskExecutionResponse(k8sApplyResponse, CommandExecutionStatus.FAILURE);
        }
      }
    }

    wrapUp(k8sDelegateTaskParams, k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, WrapUp));

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(k8sApplyResponse)
        .build();
  }

  @VisibleForTesting
  boolean init(K8sApplyTaskParameters k8sApplyTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sApplyTaskParameters.getK8sClusterConfig());

    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

    try {
      List<String> applyFilePaths = Arrays.stream(k8sApplyTaskParameters.getFilePaths().split(","))
                                        .map(String::trim)
                                        .filter(StringUtils::isNotBlank)
                                        .collect(Collectors.toList());

      if (isEmpty(applyFilePaths)) {
        executionLogCallback.saveExecutionLog(color("\nNo file specified in the state", Yellow, Bold));
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
        return false;
      }

      executionLogCallback.saveExecutionLog(color("Found following files to be applied in the state", White, Bold));
      StringBuilder sb = new StringBuilder(1024);
      applyFilePaths.forEach(each -> sb.append(color(format("- %s", each), Gray)).append(System.lineSeparator()));
      executionLogCallback.saveExecutionLog(sb.toString());

      List<ManifestFile> manifestFiles = k8sTaskHelper.renderTemplateForApply(k8sDelegateTaskParams,
          k8sApplyTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory, applyFilePaths,
          k8sApplyTaskParameters.getValuesYamlList(), releaseName, kubernetesConfig.getNamespace(),
          executionLogCallback);

      if (isEmpty(manifestFiles)) {
        executionLogCallback.saveExecutionLog(color("\nNo Manifests found after rendering", Yellow, Bold));
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
        return false;
      }

      resources = k8sTaskHelper.readManifests(manifestFiles, executionLogCallback);
      k8sTaskHelper.setNamespaceToKubernetesResourcesIfRequired(resources, kubernetesConfig.getNamespace());

      executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));
      executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

      if (k8sApplyTaskParameters.isSkipDryRun()) {
        executionLogCallback.saveExecutionLog(color("\nSkipping Dry Run", Yellow, Bold), INFO);
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      }

      return k8sTaskHelper.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
    } catch (Exception e) {
      logger.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }
  }

  private boolean prepare(ExecutionLogCallback executionLogCallback) {
    try {
      executionLogCallback.saveExecutionLog(
          "Manifests processed. Found following resources: \n" + k8sTaskHelper.getResourcesInTableFormat(resources));

      workloads = getWorkloadsForApplyState(resources);
      if (isEmpty(workloads)) {
        executionLogCallback.saveExecutionLog(color("\nNo Workload found.", Yellow, Bold));
      } else {
        executionLogCallback.saveExecutionLog(
            "Found following Workloads\n" + k8sTaskHelper.getResourcesInTableFormat(workloads));
      }
    } catch (Exception e) {
      logger.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  private void wrapUp(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    k8sTaskHelper.describe(client, k8sDelegateTaskParams, executionLogCallback);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  private K8sTaskExecutionResponse getFailureResponse() {
    K8sApplyResponse k8sApplyResponse = K8sApplyResponse.builder().build();
    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .k8sTaskResponse(k8sApplyResponse)
        .build();
  }
}
