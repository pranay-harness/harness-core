package software.wings.delegatetasks.k8s.taskhandler;

import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Rollback;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.delegatetasks.k8s.Utils.doStatusCheck;
import static software.wings.delegatetasks.k8s.Utils.getLatestRevision;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutUndoCommand;
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
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sCommandTaskParams;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sDeploymentRollingRollbackSetupRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;

import java.io.IOException;
import java.util.Collections;

@NoArgsConstructor
@Singleton
public class K8sDeploymentRollingRollbackCommandTaskHandler extends K8sCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sDeploymentRollingRollbackCommandTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  public K8sCommandExecutionResponse executeTaskInternal(
      K8sCommandRequest k8sCommandRequest, K8sCommandTaskParams k8sCommandTaskParams) throws Exception {
    if (!(k8sCommandRequest instanceof K8sDeploymentRollingRollbackSetupRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sCommandRequest", "Must be instance of K8sDeploymentRollingRollbackSetupRequest"));
    }

    K8sDeploymentRollingRollbackSetupRequest request = (K8sDeploymentRollingRollbackSetupRequest) k8sCommandRequest;

    boolean success = init(request, k8sCommandTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), Init));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = rollback(request, k8sCommandTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), Rollback));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = doStatusCheck(client, release.getManagedWorkload(), k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), WaitForSteadyState));

    if (!success) {
      releaseHistory.setReleaseStatus(Status.RollbackFailed);
      kubernetesContainerService.saveReleaseHistory(
          kubernetesConfig, Collections.emptyList(), request.getReleaseName(), releaseHistory.getAsYaml());
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    release.setStatus(Status.RollbackSucceeded);
    release.setManagedWorkloadRevision(getLatestRevision(client, release.getManagedWorkload(), k8sCommandTaskParams));

    kubernetesContainerService.saveReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName(), releaseHistory.getAsYaml());

    return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  Release release;

  private boolean init(K8sDeploymentRollingRollbackSetupRequest request, K8sCommandTaskParams k8sCommandTaskParams,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig());

    client = Kubectl.client(k8sCommandTaskParams.getKubectlPath(), k8sCommandTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    release = releaseHistory.getLatestRelease();

    executionLogCallback.saveExecutionLog("\nManaged Workload is: " + release.getManagedWorkload().kindNameRef());

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  public boolean rollback(K8sDeploymentRollingRollbackSetupRequest request, K8sCommandTaskParams k8sCommandTaskParams,
      ExecutionLogCallback executionLogCallback) throws Exception {
    if (request.getReleaseNumber() == 0) {
      executionLogCallback.saveExecutionLog("No releaseNumber found. Skipping rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    Release previousSuccessfulRelease = releaseHistory.getPreviousSuccessfulRelease(request.getReleaseNumber());

    if (previousSuccessfulRelease == null) {
      executionLogCallback.saveExecutionLog("No successful release found. Can't rollback.");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    }

    executionLogCallback.saveExecutionLog("Previous Successful Release is " + previousSuccessfulRelease.getNumber());

    executionLogCallback.saveExecutionLog("\nRolling back to release " + previousSuccessfulRelease.getNumber());

    RolloutUndoCommand rolloutUndoCommand =
        client.rollout()
            .undo()
            .resource(previousSuccessfulRelease.getManagedWorkload().kindNameRef())
            .namespace(previousSuccessfulRelease.getManagedWorkload().getNamespace())
            .toRevision(previousSuccessfulRelease.getManagedWorkloadRevision());

    executionLogCallback.saveExecutionLog("\n" + rolloutUndoCommand.command());

    try (LogOutputStream logOutputStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(line, INFO);
               }
             };

         LogOutputStream logErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(line, ERROR);
               }
             }) {
      ProcessResult result =
          rolloutUndoCommand.execute(k8sCommandTaskParams.getWorkingDirectory(), logOutputStream, logErrorStream);

      if (result.getExitValue() == 0) {
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      } else {
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
        logger.warn("Failed to rollback resource. Error {}", result.getOutput());
        return false;
      }
    }
  }
}
