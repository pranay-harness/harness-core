package io.harness.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.network.SafeHttpCall.execute;
import static io.harness.state.StateConstants.DEFAULT_STEADY_STATE_TIMEOUT;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.delegate.beans.instancesync.K8sInstanceSyncPerpetualTaskResponse;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.mapper.K8sPodToServiceInstanceInfoMapper;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.grpc.utils.AnyUtils;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.K8sContainerInstanceSyncPerpetualTaskParams;
import io.harness.serializer.KryoSerializer;

import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class K8sInstanceSyncPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the K8s InstanceSync perpetual task executor for task id: {}", taskId);
    K8sContainerInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), K8sContainerInstanceSyncPerpetualTaskParams.class);
    return executeK8sInstanceSyncTask(taskId, taskParams);
  }

  private PerpetualTaskResponse executeK8sInstanceSyncTask(
      PerpetualTaskId taskId, K8sContainerInstanceSyncPerpetualTaskParams taskParams) {
    final K8sClusterConfig k8sClusterConfig =
        (K8sClusterConfig) kryoSerializer.asObject(taskParams.getK8SClusterConfig().toByteArray());
    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig, true);

    K8sInstanceSyncPerpetualTaskResponse k8sInstanceSyncTaskResponse =
        getK8sInstanceSyncTaskResponse(taskParams, kubernetesConfig);
    publishInstanceSyncResult(
        taskId, taskParams.getAccountId(), taskParams.getNamespace(), k8sInstanceSyncTaskResponse);

    boolean isFailureResponse = FAILURE == k8sInstanceSyncTaskResponse.getCommandExecutionStatus();
    return PerpetualTaskResponse.builder()
        .responseCode(SC_OK)
        .responseMessage(isFailureResponse ? k8sInstanceSyncTaskResponse.getErrorMessage() : "success")
        .build();
  }

  private K8sInstanceSyncPerpetualTaskResponse getK8sInstanceSyncTaskResponse(
      K8sContainerInstanceSyncPerpetualTaskParams k8sContainerInstanceSyncPerpetualTaskParams,
      KubernetesConfig kubernetesConfig) {
    long timeoutMillis = K8sTaskHelperBase.getTimeoutMillisFromMinutes(DEFAULT_STEADY_STATE_TIMEOUT);
    String namespace = k8sContainerInstanceSyncPerpetualTaskParams.getNamespace();
    String releaseName = k8sContainerInstanceSyncPerpetualTaskParams.getReleaseName();
    try {
      List<K8sPod> k8sPodList =
          k8sTaskHelperBase.getPodDetails(kubernetesConfig, namespace, releaseName, timeoutMillis);
      List<ServerInstanceInfo> serverInstanceDetails =
          K8sPodToServiceInstanceInfoMapper.toServerInstanceInfoList(k8sPodList);

      return K8sInstanceSyncPerpetualTaskResponse.builder()
          .serverInstanceDetails(serverInstanceDetails)
          .commandExecutionStatus(isEmpty(k8sPodList) ? SUCCESS : FAILURE)
          .build();

    } catch (Exception exception) {
      log.error(String.format("Failed to fetch k8s pod list for namespace: [%s] and releaseName:[%s] ",
                    k8sContainerInstanceSyncPerpetualTaskParams.getNamespace(),
                    k8sContainerInstanceSyncPerpetualTaskParams.getReleaseName()),
          exception);
      return K8sInstanceSyncPerpetualTaskResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage(exception.getMessage())
          .build();
    }
  }

  private void publishInstanceSyncResult(
      PerpetualTaskId taskId, String accountId, String namespace, InstanceSyncPerpetualTaskResponse responseData) {
    try {
      execute(delegateAgentManagerClient.processInstanceSyncNGResult(taskId.getId(), accountId, responseData));
    } catch (Exception e) {
      log.error(String.format("Failed to publish K8s instance sync result. namespace [%s] and PerpetualTaskId [%s]",
                    namespace, taskId.getId()),
          e);
    }
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
