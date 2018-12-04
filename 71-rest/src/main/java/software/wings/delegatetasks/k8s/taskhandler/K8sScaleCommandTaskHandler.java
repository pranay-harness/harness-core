package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.Scale;
import static software.wings.beans.command.K8sDummyCommandUnit.WaitForSteadyState;
import static software.wings.delegatetasks.k8s.Utils.doStatusCheck;
import static software.wings.delegatetasks.k8s.Utils.getCurrentReplicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.ScaleCommand;
import io.harness.k8s.model.Release;
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
import software.wings.helpers.ext.k8s.request.K8sScaleRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sScaleResponse;

import java.util.Collections;

@NoArgsConstructor
@Singleton
public class K8sScaleCommandTaskHandler extends K8sCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sScaleCommandTaskHandler.class);
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  private KubernetesConfig kubernetesConfig;
  private Kubectl client;
  private ReleaseHistory releaseHistory;
  private int targetReplicaCount;
  Release release;

  public K8sCommandExecutionResponse executeTaskInternal(
      K8sCommandRequest k8sCommandRequest, K8sCommandTaskParams k8sCommandTaskParams) throws Exception {
    if (!(k8sCommandRequest instanceof K8sScaleRequest)) {
      throw new InvalidArgumentsException(Pair.of("k8sCommandRequest", "Must be instance of K8sScaleRequest"));
    }

    K8sScaleRequest request = (K8sScaleRequest) k8sCommandRequest;

    boolean success = init(request, k8sCommandTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), Init));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = scale(request, k8sCommandTaskParams,
        new ExecutionLogCallback(
            delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), Scale));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    success = doStatusCheck(client, release.getManagedWorkload(), k8sCommandTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sCommandRequest.getAccountId(), k8sCommandRequest.getAppId(),
            k8sCommandRequest.getActivityId(), WaitForSteadyState));

    if (!success) {
      return K8sCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    return K8sCommandExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sCommandResponse(K8sScaleResponse.builder().build())
        .build();
  }

  private boolean init(K8sScaleRequest request, K8sCommandTaskParams k8sCommandTaskParams,
      ExecutionLogCallback executionLogCallback) throws Exception {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(request.getK8sClusterConfig());

    client = Kubectl.client(k8sCommandTaskParams.getKubectlPath(), k8sCommandTaskParams.getKubeconfigPath());

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, Collections.emptyList(), request.getReleaseName());

    releaseHistory = (StringUtils.isEmpty(releaseHistoryData)) ? ReleaseHistory.createNew()
                                                               : ReleaseHistory.createFromData(releaseHistoryData);

    release = releaseHistory.getLatestRelease();

    executionLogCallback.saveExecutionLog("\nManaged Workload is: " + release.getManagedWorkload().kindNameRef());

    executionLogCallback.saveExecutionLog("\nQuerying current replicas");
    int currentReplicas = getCurrentReplicas(client, release.getManagedWorkload(), k8sCommandTaskParams);
    executionLogCallback.saveExecutionLog("Current replica count is " + currentReplicas);

    switch (request.getInstanceUnitType()) {
      case COUNT:
        targetReplicaCount = request.getInstances();
        break;

      case PERCENTAGE:
        int maxInstances;
        if (request.getMaxInstances().isPresent()) {
          maxInstances = request.getMaxInstances().get();
        } else {
          maxInstances = currentReplicas;
        }
        targetReplicaCount = (int) Math.round(request.getInstances() * maxInstances / 100.0);
        break;

      default:
        unhandled(request.getInstanceUnitType());
    }

    executionLogCallback.saveExecutionLog("Target replica count is " + targetReplicaCount);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }

  public boolean scale(K8sScaleRequest request, K8sCommandTaskParams k8sCommandTaskParams,
      ExecutionLogCallback executionLogCallback) throws Exception {
    executionLogCallback.saveExecutionLog("\nScaling " + request.getResource());

    ScaleCommand scaleCommand = client.scale().resource(request.getResource()).replicas(targetReplicaCount);

    executionLogCallback.saveExecutionLog("\n" + scaleCommand.command());

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
          scaleCommand.execute(k8sCommandTaskParams.getWorkingDirectory(), logOutputStream, logErrorStream);

      if (result.getExitValue() == 0) {
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      } else {
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
        logger.warn("Failed to scale resource. Error {}", result.getOutput());
        return false;
      }
    }
  }
}
