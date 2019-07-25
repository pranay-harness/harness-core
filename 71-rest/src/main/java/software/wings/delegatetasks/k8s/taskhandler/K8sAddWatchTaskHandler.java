package software.wings.delegatetasks.k8s.taskhandler;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.watch.K8sWatchService;
import software.wings.delegatetasks.k8s.watch.WatchRequest;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sAddWatchResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.concurrent.TimeUnit;

/**
 * This class is for manual testing purpose only, not for production.
 */
public class K8sAddWatchTaskHandler extends K8sTaskHandler {
  @Inject private K8sWatchService k8sWatchService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    // construct the watch request
    WatchRequest watchRequest = WatchRequest.builder()
                                    .k8sClusterConfig(k8sTaskParameters.getK8sClusterConfig())
                                    .namespace(k8sTaskParameters.getK8sClusterConfig().getNamespace())
                                    .k8sResourceKind("Pod")
                                    .build();

    k8sWatchService.register(watchRequest);
    TimeUnit.MINUTES.sleep(5);

    K8sAddWatchResponse k8sApplyResponse = K8sAddWatchResponse.builder().build();

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(k8sApplyResponse)
        .build();
  }
}
