package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.filesystem.FileIo.writeUtf8StringToFile;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.k8s.taskhandler.K8sTaskHandler;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class K8sTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, K8sTaskHandler> k8sCommandTaskTypeToTaskHandlerMap;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;
  private static final String WORKING_DIR_BASE = "./repository/k8s/";
  private static final String KUBECONFIG_FILENAME = "config";

  private static final Logger logger = LoggerFactory.getLogger(K8sTask.class);

  public K8sTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
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
        logger.error(format("Exception in processing k8s task [%s]", k8sTaskParameters.toString()), ex);
        return K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .build();
      }
    } else {
      String workingDirectory =
          Paths.get(WORKING_DIR_BASE, convertBase64UuidToCanonicalForm(k8sTaskParameters.getWorkflowExecutionId()))
              .normalize()
              .toAbsolutePath()
              .toString();

      try {
        String kubeconfigFileContent =
            containerDeploymentDelegateHelper.getKubeconfigFileContent(k8sTaskParameters.getK8sClusterConfig());

        createDirectoryIfDoesNotExist(workingDirectory);
        waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);
        writeUtf8StringToFile(Paths.get(workingDirectory, KUBECONFIG_FILENAME).toString(), kubeconfigFileContent);

        K8sDelegateTaskParams k8SDelegateTaskParams =
            K8sDelegateTaskParams.builder()
                .kubectlPath(k8sGlobalConfigService.getKubectlPath())
                .kubeconfigPath(KUBECONFIG_FILENAME)
                .workingDirectory(workingDirectory)
                .goTemplateClientPath(k8sGlobalConfigService.getGoTemplateClientPath())
                .helmPath(k8sGlobalConfigService.getHelmPath())
                .build();

        return k8sCommandTaskTypeToTaskHandlerMap.get(k8sTaskParameters.getCommandType().name())
            .executeTask(k8sTaskParameters, k8SDelegateTaskParams);
      } catch (Exception ex) {
        logger.error(format("Exception in processing k8s task [%s]", k8sTaskParameters.toString()), ex);
        return K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .build();
      } finally {
        cleanup(workingDirectory);
      }
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
