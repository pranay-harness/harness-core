package software.wings.delegatetasks.k8s.taskhandler;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.KubernetesConfig;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.delegatetasks.k8s.watch.K8sWatchService;
import software.wings.delegatetasks.k8s.watch.WatchRequest;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sAddWatchResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is for manual testing purpose only, not for production.
 */
public class K8sAddWatchTaskHandler extends K8sTaskHandler {
  @Inject private K8sTaskHelper k8sTaskHelper;
  @Inject private transient K8sWatchService k8sWatchService;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private ExecutorService executorService;

  private KubernetesConfig kubernetesConfig;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    // K8sAddWatchTaskParameters k8sAddWatchTaskParameters = (K8sAddWatchTaskParameters) k8sTaskParameters;

    // construct the watch request
    WatchRequest watchRequest = WatchRequest.builder()
                                    .k8sClusterConfig(k8sTaskParameters.getK8sClusterConfig())
                                    .namespace(k8sTaskParameters.getK8sClusterConfig().getNamespace())
                                    .k8sResourceKind("Pod")
                                    .build();

    CountDownLatch closeLatch = new CountDownLatch(1);

    k8sWatchService.register(watchRequest);

    closeLatch.await(5, TimeUnit.MINUTES);

    K8sAddWatchResponse k8sApplyResponse = K8sAddWatchResponse.builder().build();

    return K8sTaskExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sTaskResponse(k8sApplyResponse)
        .build();
  }
}
