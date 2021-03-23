package io.harness.perpetualtask.instancesyncng;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.state.StateConstants.DEFAULT_STEADY_STATE_TIMEOUT;

import static software.wings.service.impl.ContainerMetadataType.K8S;

import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sScaleResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.K8sYamlToDelegateDTOMapper;
import io.harness.grpc.utils.AnyUtils;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.instancesync.InstanceSyncPerpetualTaskParamsNg.InstanceSyncPerpetualTaskParamsNG;
import io.harness.perpetualtask.instancesync.InstanceSyncPerpetualTaskParamsNg.K8sInstanceSyncPerpetualTaskParamsNG;
import io.harness.serializer.KryoSerializer;

import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.intfc.ContainerService;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;

// todo: Add publishInstanceSyncResult method
@Slf4j
public class K8sInstanceSyncPerpetualTaskExecutorNG implements PerpetualTaskExecutor {
  @Inject private transient KryoSerializer kryoSerializer;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private transient ContainerService containerService;
  @Inject private transient K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);
    InstanceSyncPerpetualTaskParamsNG taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), InstanceSyncPerpetualTaskParamsNG.class);

    return K8S.name().equals(taskParams.getContainerType())
        ? executeK8sContainerSyncTask(taskId, taskParams.getK8SPerpetualTaskParams())
        : null;
  }

  private PerpetualTaskResponse executeK8sContainerSyncTask(
      PerpetualTaskId taskId, K8sInstanceSyncPerpetualTaskParamsNG k8SContainerPerpetualTaskParams) {
    final KubernetesClusterConfigDTO k8sClusterConfig = (KubernetesClusterConfigDTO) kryoSerializer.asObject(
        k8SContainerPerpetualTaskParams.getK8SClusterConfig().toByteArray());
    KubernetesConfig kubernetesConfig = k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(
        k8sClusterConfig, k8SContainerPerpetualTaskParams.getNamespace());

    K8sDeployResponse responseData = getK8sTaskResponse(k8SContainerPerpetualTaskParams, kubernetesConfig);
    //     publishInstanceSyncResult(taskId, k8sContainerInstanceSyncPerpetualTaskParams.getAccountId(),
    //        k8sContainerInstanceSyncPerpetualTaskParams.getNamespace(), responseData);
    boolean isFailureResponse = FAILURE == responseData.getCommandExecutionStatus();
    return PerpetualTaskResponse.builder()
        .responseCode(Response.SC_OK)
        .responseMessage(isFailureResponse ? responseData.getErrorMessage() : "success")
        .build();
  }
  private K8sDeployResponse getK8sTaskResponse(
      K8sInstanceSyncPerpetualTaskParamsNG k8SContainerPerpetualTaskParams, KubernetesConfig kubernetesConfig) {
    try {
      long timeoutMillis = K8sTaskHelperBase.getTimeoutMillisFromMinutes(DEFAULT_STEADY_STATE_TIMEOUT);
      String namespace = k8SContainerPerpetualTaskParams.getNamespace();
      String releaseName = k8SContainerPerpetualTaskParams.getReleaseName();
      List<K8sPod> k8sPodList =
          k8sTaskHelperBase.getPodDetails(kubernetesConfig, namespace, releaseName, timeoutMillis);

      return K8sDeployResponse.builder()
          .k8sNGTaskResponse(K8sScaleResponse.builder().k8sPodList(k8sPodList).build())
          .commandExecutionStatus((k8sPodList != null) ? SUCCESS : FAILURE)
          .build();

    } catch (Exception exception) {
      log.error(String.format("Failed to fetch k8s pod list for namespace: [%s] and releaseName:[%s] ",
                    k8SContainerPerpetualTaskParams.getNamespace(), k8SContainerPerpetualTaskParams.getReleaseName()),
          exception);
      return K8sDeployResponse.builder().commandExecutionStatus(FAILURE).errorMessage(exception.getMessage()).build();
    }
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
