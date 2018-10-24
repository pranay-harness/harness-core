package software.wings.delegatetasks.k8s;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.writeUtf8StringToFile;
import static java.lang.String.format;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.k8s.taskhandler.K8sCommandTaskHandler;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;
import software.wings.utils.Misc;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class K8sCommandTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, K8sCommandTaskHandler> k8sCommandTaskTypeToTaskHandlerMap;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  private static final String WORKING_DIR_BASE = "./repository/k8s/";
  private static final String KUBECONFIG_FILENAME = ".kubeconfig";

  private static final Logger logger = LoggerFactory.getLogger(K8sCommandTask.class);

  public K8sCommandTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public K8sCommandExecutionResponse run(Object[] parameters) {
    K8sCommandRequest k8sCommandRequest = (K8sCommandRequest) parameters[0];

    String workingDirectory =
        Paths.get(WORKING_DIR_BASE, k8sCommandRequest.getWorkflowExecutionId()).normalize().toAbsolutePath().toString();

    try {
      String kubeconfigFileContent =
          containerDeploymentDelegateHelper.getKubeconfigFileContent(k8sCommandRequest.getK8sClusterConfig());

      createDirectoryIfDoesNotExist(workingDirectory);
      writeUtf8StringToFile(Paths.get(workingDirectory, KUBECONFIG_FILENAME).toString(), kubeconfigFileContent);

      K8sCommandTaskParams k8sCommandTaskParams =
          K8sCommandTaskParams.builder().kubeconfigPath(KUBECONFIG_FILENAME).workingDirectory(workingDirectory).build();

      return k8sCommandTaskTypeToTaskHandlerMap.get(k8sCommandRequest.getCommandType().name())
          .executeTask(k8sCommandRequest, k8sCommandTaskParams);
    } catch (Exception ex) {
      logger.error(format("Exception in processing k8s task [%s]", k8sCommandRequest.toString()), ex);
      return K8sCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(Misc.getMessage(ex))
          .build();
    } finally {
      cleanup(workingDirectory);
    }
  }

  private void cleanup(String workingDirectory) {
    try {
      deleteDirectoryAndItsContentIfExists(workingDirectory);
    } catch (Exception ex) {
      logger.warn("Exception in directory cleanup.", ex);
    }
  }
}
