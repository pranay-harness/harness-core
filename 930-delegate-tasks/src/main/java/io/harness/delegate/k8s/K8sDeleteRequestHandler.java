package io.harness.delegate.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getResourcesInStringFormat;
import static io.harness.delegate.task.k8s.K8sTaskHelperBase.getTimeoutMillisFromMinutes;
import static io.harness.k8s.K8sCommandUnitConstants.Delete;
import static io.harness.k8s.K8sCommandUnitConstants.FetchFiles;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.k8s.model.KubernetesResourceId.createKubernetesResourceIdsFromKindName;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Gray;
import static software.wings.beans.LogColor.GrayDark;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
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
import io.harness.logging.LogCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Slf4j
public class K8sDeleteRequestHandler extends K8sRequestHandler {
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private K8sDeleteBaseHandler k8sDeleteBaseHandler;

  private Kubectl client;
  private String releaseName;
  private String manifestFilesDirectory;
  @Nullable private List<KubernetesResource> resources;
  private List<KubernetesResourceId> resourceIdsToDelete;
  private KubernetesConfig kubernetesConfig;

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
                                                  K8sDelegateTaskParams k8SDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(k8sDeployRequest instanceof K8sDeleteRequest)) {
      throw new InvalidArgumentsException(Pair.of("k8sDeployRequest", "Must be instance of K8sDeleteRequest"));
    }

    K8sDeleteRequest k8sDeleteRequest = (K8sDeleteRequest) k8sDeployRequest;
    releaseName = k8sDeleteRequest.getReleaseName();
    manifestFilesDirectory = Paths.get(k8SDelegateTaskParams.getWorkingDirectory(), MANIFEST_FILES_DIR).toString();
    LogCallback executionLogCallback = k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Delete, true, commandUnitsProgress);

    if (isEmpty(k8sDeleteRequest.getResources())) {
      return executeDeleteUsingFiles(
          k8sDeleteRequest, k8SDelegateTaskParams, executionLogCallback, logStreamingTaskClient);
    } else if (!isEmpty(k8sDeleteRequest.getFilePaths())) {
      executionLogCallback.saveExecutionLog("Both resources and files are present, giving priority to resources.");
    }

    return executeDeleteUsingResources(
        k8sDeleteRequest, k8SDelegateTaskParams, executionLogCallback, logStreamingTaskClient);
  }

  private K8sDeployResponse executeDeleteUsingFiles(K8sDeleteRequest k8sDeleteRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback,
      ILogStreamingTaskClient logStreamingTaskClient) throws Exception {
    long steadyStateTimeoutInMillis = getTimeoutMillisFromMinutes(k8sDeleteRequest.getTimeoutIntervalInMin());

    boolean success =
        k8sTaskHelperBase.fetchManifestFilesAndWriteToDirectory(k8sDeleteRequest.getManifestDelegateConfig(),
            manifestFilesDirectory, k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, FetchFiles, true, null),
            steadyStateTimeoutInMillis, k8sDeleteRequest.getAccountId());
    if (!success) {
      return k8sDeleteBaseHandler.getFailureResponse();
    }
    success = initUsingFilePaths(
        k8sDeleteRequest, k8sDelegateTaskParams, k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Init, true, null));
    if (!success) {
      return k8sDeleteBaseHandler.getFailureResponse();
    }
    k8sTaskHelperBase.deleteManifests(client, resources, k8sDelegateTaskParams, executionLogCallback);
    return k8sDeleteBaseHandler.getSuccessResponse();
  }

  @VisibleForTesting
  boolean initUsingFilePaths(K8sDeleteRequest k8sDeleteRequest, K8sDelegateTaskParams k8sDelegateTaskParams,
      LogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    try {
      client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

      if (isEmpty(k8sDeleteRequest.getFilePaths())) {
        executionLogCallback.saveExecutionLog(color("\nNo file specified in the state", Yellow, Bold));
        executionLogCallback.saveExecutionLog("\nSuccess.", INFO, SUCCESS);
        return true;
      }
      List<String> deleteFilePaths = Arrays.stream(k8sDeleteRequest.getFilePaths().split(","))
                                         .map(String::trim)
                                         .filter(StringUtils::isNotBlank)
                                         .collect(Collectors.toList());

      if (isEmpty(deleteFilePaths)) {
        executionLogCallback.saveExecutionLog(color("\nNo file specified in the state", Yellow, Bold));
        executionLogCallback.saveExecutionLog("\nSuccess.", INFO, SUCCESS);
        return true;
      }

      executionLogCallback.saveExecutionLog(color("Found following files to be applied in the state", White, Bold));
      StringBuilder sb = new StringBuilder(1024);
      deleteFilePaths.forEach(each -> sb.append(color(format("- %s", each), Gray)).append(System.lineSeparator()));
      executionLogCallback.saveExecutionLog(sb.toString());

      resources = k8sTaskHelperBase.getResourcesFromManifests(k8sDelegateTaskParams,
          k8sDeleteRequest.getManifestDelegateConfig(), manifestFilesDirectory, deleteFilePaths,
          k8sDeleteRequest.getValuesYamlList(), releaseName,
          k8sDeleteRequest.getK8sInfraDelegateConfig().getNamespace(), executionLogCallback,
          k8sDeleteRequest.getTimeoutIntervalInMin());

      executionLogCallback.saveExecutionLog(color("\nManifests [Post template rendering] :\n", White, Bold));
      executionLogCallback.saveExecutionLog(ManifestHelper.toYamlForLogs(resources));
      return true;
    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, FAILURE);
      return false;
    }
  }

  private K8sDeployResponse executeDeleteUsingResources(K8sDeleteRequest k8sDeleteRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback,
      ILogStreamingTaskClient logStreamingTaskClient) throws Exception {
    boolean success = init(
        k8sDeleteRequest, k8sDelegateTaskParams, k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Init, true, null));
    if (!success) {
      return k8sDeleteBaseHandler.getFailureResponse();
    }
    if (isEmpty(resourceIdsToDelete)) {
      return k8sDeleteBaseHandler.getSuccessResponse();
    }
    k8sTaskHelperBase.delete(client, k8sDelegateTaskParams, resourceIdsToDelete, executionLogCallback, true);
    return k8sDeleteBaseHandler.getSuccessResponse();
  }

  private boolean init(K8sDeleteRequest k8sDeleteRequest, K8sDelegateTaskParams k8sDelegateTaskParams,
      LogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..\n");
    client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());
    kubernetesConfig =
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(k8sDeleteRequest.getK8sInfraDelegateConfig());
    try {
      if (StringUtils.isEmpty(k8sDeleteRequest.getResources())) {
        executionLogCallback.saveExecutionLog("\nNo resources found to delete.");
        return true;
      }

      if ("*".equals(k8sDeleteRequest.getResources().trim())) {
        executionLogCallback.saveExecutionLog("All Resources are selected for deletion");
        executionLogCallback.saveExecutionLog(color("Delete Namespace is set to: "
                + k8sDeleteRequest.isDeleteNamespacesForRelease() + ", Skipping deleting Namespace resources",
            GrayDark, Bold));
        executionLogCallback.saveExecutionLog(
            "Delete Namespace is set to: " + k8sDeleteRequest.isDeleteNamespacesForRelease());
        resourceIdsToDelete =
            k8sDeleteBaseHandler.getResourceIdsForDeletion(k8sDeleteRequest, kubernetesConfig, executionLogCallback);
      } else {
        resourceIdsToDelete = createKubernetesResourceIdsFromKindName(k8sDeleteRequest.getResources());
      }

      executionLogCallback.saveExecutionLog(color("\nResources to delete are: ", White, Bold)
          + color(getResourcesInStringFormat(resourceIdsToDelete), Gray));

      executionLogCallback.saveExecutionLog("Done.", INFO, SUCCESS);

      return true;
    } catch (Exception e) {
      log.error("Exception:", e);
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }
}
