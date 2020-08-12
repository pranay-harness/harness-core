package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.filesystem.FileIo.writeUtf8StringToFile;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.ExceptionUtils;
import io.harness.k8s.K8sConstants;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.k8s.taskhandler.K8sTaskHandler;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
@Slf4j
public class K8sTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, K8sTaskHandler> k8sCommandTaskTypeToTaskHandlerMap;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  private static final String WORKING_DIR_BASE = "./repository/k8s/";

  public K8sTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public K8sTaskExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public K8sTaskExecutionResponse run(TaskParameters parameters) {
    K8sTaskParameters k8sTaskParameters = (K8sTaskParameters) parameters;

    logger.info("Starting task execution for Command {}", k8sTaskParameters.getCommandType().name());

    if (k8sTaskParameters.getCommandType() == K8sTaskType.INSTANCE_SYNC) {
      try {
        return k8sCommandTaskTypeToTaskHandlerMap.get(k8sTaskParameters.getCommandType().name())
            .executeTask(k8sTaskParameters, null);
      } catch (Exception ex) {
        logger.error("Exception in processing k8s task [{}]", k8sTaskParameters.toString(), ex);
        return K8sTaskExecutionResponse.builder()
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
        String kubeconfigFileContent =
            containerDeploymentDelegateHelper.getKubeconfigFileContent(k8sTaskParameters.getK8sClusterConfig());

        createDirectoryIfDoesNotExist(workingDirectory);
        waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);
        writeUtf8StringToFile(
            Paths.get(workingDirectory, K8sConstants.KUBECONFIG_FILENAME).toString(), kubeconfigFileContent);

        createDirectoryIfDoesNotExist(Paths.get(workingDirectory, K8sConstants.MANIFEST_FILES_DIR).toString());

        K8sDelegateTaskParams k8SDelegateTaskParams =
            K8sDelegateTaskParams.builder()
                .kubectlPath(k8sGlobalConfigService.getKubectlPath())
                .kubeconfigPath(K8sConstants.KUBECONFIG_FILENAME)
                .workingDirectory(workingDirectory)
                .goTemplateClientPath(k8sGlobalConfigService.getGoTemplateClientPath())
                .helmPath(k8sGlobalConfigService.getHelmPath(k8sTaskParameters.getHelmVersion()))
                .ocPath(k8sGlobalConfigService.getOcPath())
                .kustomizeBinaryPath(k8sGlobalConfigService.getKustomizePath())
                .build();

        logK8sVersion(k8sTaskParameters, k8SDelegateTaskParams);

        return k8sCommandTaskTypeToTaskHandlerMap.get(k8sTaskParameters.getCommandType().name())
            .executeTask(k8sTaskParameters, k8SDelegateTaskParams);
      } catch (Exception ex) {
        logger.error("Exception in processing k8s task [{}]", k8sTaskParameters.toString(), ex);
        return K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .build();
      } finally {
        cleanup(workingDirectory);
      }
    }
  }

  private void logK8sVersion(K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) {
    try {
      k8sCommandTaskTypeToTaskHandlerMap.get(K8sTaskType.VERSION.name())
          .executeTask(k8sTaskParameters, k8sDelegateTaskParams);
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
