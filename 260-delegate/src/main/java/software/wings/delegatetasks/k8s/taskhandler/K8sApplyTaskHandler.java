package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.k8s.K8sCommandUnitConstants.Apply;
import static io.harness.k8s.K8sCommandUnitConstants.FetchFiles;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Prepare;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.k8s.K8sCommandUnitConstants.WrapUp;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.k8s.manifest.ManifestHelper.getCustomResourceDefinitionWorkloads;
import static io.harness.k8s.manifest.ManifestHelper.getEligibleWorkloads;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Gray;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sApplyResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Slf4j
public class K8sApplyTaskHandler extends K8sTaskHandler {
  @Inject private K8sTaskHelper k8sTaskHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  private Kubectl client;
  private String releaseName;
  private List<KubernetesResource> resources;
  private List<KubernetesResource> workloads;
  private List<KubernetesResource> customWorkloads;
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
    final long timeoutInMillis = getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());

    K8sApplyResponse k8sApplyResponse = K8sApplyResponse.builder().build();

    boolean success = k8sTaskHelper.fetchManifestFilesAndWriteToDirectory(
        k8sApplyTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory,
        k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, FetchFiles), timeoutInMillis);
    if (!success) {
      return getFailureResponse();
    }

    success = init(k8sApplyTaskParameters, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, Init));
    if (!success) {
      return getFailureResponse();
    }

    success = prepare(k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, Prepare), k8sApplyTaskParameters);
    if (!success) {
      return getFailureResponse();
    }

    success = k8sTaskHelperBase.applyManifests(client, resources, k8sDelegateTaskParams,
        k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, Apply), true);
    if (!success) {
      return getFailureResponse();
    }

    if (isEmpty(workloads) && isEmpty(customWorkloads)) {
      k8sTaskHelper.getExecutionLogCallback(k8sApplyTaskParameters, WaitForSteadyState)
          .saveExecutionLog("Skipping Status Check since there is no Workload.", INFO, SUCCESS);
    } else {
      if (!k8sApplyTaskParameters.isSkipSteadyStateCheck()) {
        List<KubernetesResourceId> kubernetesResourceIds =
            workloads.stream().map(KubernetesResource::getResourceId).collect(Collectors.toList());

        ExecutionLogCallback executionLogCallback =
            new ExecutionLogCallback(delegateLogService, k8sTaskParameters.getAccountId(), k8sTaskParameters.getAppId(),
                k8sTaskParameters.getActivityId(), WaitForSteadyState);
        success = k8sTaskHelperBase.doStatusCheckForAllResources(client, kubernetesResourceIds, k8sDelegateTaskParams,
            k8sTaskParameters.getK8sClusterConfig().getNamespace(), executionLogCallback, customWorkloads.isEmpty());

        boolean customResourcesStatusSuccess = k8sTaskHelperBase.doStatusCheckForAllCustomResources(
            client, customWorkloads, k8sDelegateTaskParams, executionLogCallback, true, timeoutInMillis);

        if (!success || !customResourcesStatusSuccess) {
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
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sApplyTaskParameters.getK8sClusterConfig(), false);

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

      resources = k8sTaskHelper.getResourcesFromManifests(k8sDelegateTaskParams,
          k8sApplyTaskParameters.getK8sDelegateManifestConfig(), manifestFilesDirectory, applyFilePaths,
          k8sApplyTaskParameters.getValuesYamlList(), releaseName, kubernetesConfig.getNamespace(),
          executionLogCallback, k8sApplyTaskParameters);

      executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));
      executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));

      if (k8sApplyTaskParameters.isSkipDryRun()) {
        executionLogCallback.saveExecutionLog(color("\nSkipping Dry Run", Yellow, Bold), INFO);
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      }

      return k8sTaskHelperBase.dryRunManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }
  }

  @VisibleForTesting
  boolean prepare(ExecutionLogCallback executionLogCallback, K8sApplyTaskParameters k8sApplyTaskParameters) {
    try {
      executionLogCallback.saveExecutionLog("Manifests processed. Found following resources: \n"
          + k8sTaskHelperBase.getResourcesInTableFormat(resources));

      workloads = getEligibleWorkloads(resources);
      customWorkloads = getCustomResourceDefinitionWorkloads(resources);
      if (isEmpty(workloads) && isEmpty(customWorkloads)) {
        executionLogCallback.saveExecutionLog(color("\nNo Workload found.", Yellow, Bold));
      } else {
        executionLogCallback.saveExecutionLog("Found following Workloads\n"
            + k8sTaskHelperBase.getResourcesInTableFormat(ListUtils.union(workloads, customWorkloads)));
        if (!k8sApplyTaskParameters.isSkipSteadyStateCheck() && !customWorkloads.isEmpty()) {
          k8sTaskHelperBase.checkSteadyStateCondition(customWorkloads);
        }
      }
    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  private void wrapUp(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    k8sTaskHelperBase.describe(client, k8sDelegateTaskParams, executionLogCallback);

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
