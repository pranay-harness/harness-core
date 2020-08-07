package io.harness.cdng.k8s.rolling;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.filesystem.FileIo.writeUtf8StringToFile;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.k8s.K8sRequestHandler;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.ExceptionUtils;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
@Slf4j
public class K8sTaskNG extends AbstractDelegateRunnableTask {
  @Inject private Map<String, K8sRequestHandler> k8sTaskTypeToRequestHandler;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  private static final String WORKING_DIR_BASE = "./repository/k8s/";
  public static final String KUBECONFIG_FILENAME = "config";
  public static final String MANIFEST_FILES_DIR = "manifest-files";

  public K8sTaskNG(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public K8sTaskExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public K8sDeployResponse run(TaskParameters parameters) {
    K8sDeployRequest k8sDeployRequest = (K8sDeployRequest) parameters;

    logger.info("Starting task execution for Command {}", k8sDeployRequest.getTaskType().name());

    if (k8sDeployRequest.getTaskType() == K8sTaskType.INSTANCE_SYNC) {
      try {
        return k8sTaskTypeToRequestHandler.get(k8sDeployRequest.getTaskType().name())
            .executeTask(k8sDeployRequest, null);
      } catch (Exception ex) {
        logger.error("Exception in processing k8s task [{}]", k8sDeployRequest.toString(), ex);
        return K8sDeployResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .build();
      }
    } else {
      String workingDirectory = Paths.get(WORKING_DIR_BASE, convertBase64UuidToCanonicalForm(generateUuid()))
                                    .normalize()
                                    .toAbsolutePath()
                                    .toString();

      try {
        // TODO: fix this later with containerDeploymentDelegateHelper service
        String kubeconfigFileContent = containerDeploymentDelegateHelper.getKubeconfigFileContent(null);
        kubeconfigFileContent = "";

        createDirectoryIfDoesNotExist(workingDirectory);
        waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);
        writeUtf8StringToFile(Paths.get(workingDirectory, KUBECONFIG_FILENAME).toString(), kubeconfigFileContent);

        createDirectoryIfDoesNotExist(Paths.get(workingDirectory, MANIFEST_FILES_DIR).toString());

        K8sDelegateTaskParams k8SDelegateTaskParams =
            K8sDelegateTaskParams.builder()
                .kubectlPath(k8sGlobalConfigService.getKubectlPath())
                .kubeconfigPath(KUBECONFIG_FILENAME)
                .workingDirectory(workingDirectory)
                .goTemplateClientPath(k8sGlobalConfigService.getGoTemplateClientPath())
                // TODO: later add helm versions support also
                .helmPath(k8sGlobalConfigService.getHelmPath(null))
                .ocPath(k8sGlobalConfigService.getOcPath())
                .kustomizeBinaryPath(k8sGlobalConfigService.getKustomizePath())
                .build();

        logK8sVersion(k8sDeployRequest, k8SDelegateTaskParams);

        return k8sTaskTypeToRequestHandler.get(k8sDeployRequest.getTaskType().name())
            .executeTask(k8sDeployRequest, k8SDelegateTaskParams);
      } catch (Exception ex) {
        logger.error("Exception in processing k8s task [{}]", k8sDeployRequest.toString(), ex);
        return K8sDeployResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .build();
      } finally {
        cleanup(workingDirectory);
      }
    }
  }

  private void logK8sVersion(K8sDeployRequest k8sDeployRequest, K8sDelegateTaskParams k8sDelegateTaskParams) {
    try {
      k8sTaskTypeToRequestHandler.get(K8sTaskType.VERSION.name()).executeTask(k8sDeployRequest, k8sDelegateTaskParams);
    } catch (Exception ex) {
      logger.error("Error fetching K8s Server Version: ", ex);
    }
  }

  private void cleanup(String workingDirectory) {
    try {
      logger.warn("Cleaning up directory " + workingDirectory);
      deleteDirectoryAndItsContentIfExists(workingDirectory);
    } catch (Exception ex) {
      logger.warn("Exception in directory cleanup.", ex);
    }
  }
}
