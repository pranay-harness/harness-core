package io.harness.perpetualtask;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.network.SafeHttpCall.execute;
import static io.harness.perpetualtask.PerpetualTaskState.TASK_RUN_FAILED;
import static io.harness.perpetualtask.PerpetualTaskState.TASK_RUN_SUCCEEDED;
import static software.wings.service.impl.ContainerMetadataType.K8S;

import com.google.inject.Inject;

import io.harness.delegate.beans.ResponseData;
import io.harness.grpc.utils.AnyUtils;
import io.harness.k8s.model.K8sPod;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.ContainerInstanceSyncPerpetualTaskParams;
import io.harness.perpetualtask.instancesync.ContainerServicePerpetualTaskParams;
import io.harness.perpetualtask.instancesync.K8sContainerInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.response.K8sInstanceSyncResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.ContainerService;

import java.time.Instant;
import java.util.List;

@Slf4j
public class ContainerInstanceSyncPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private transient K8sTaskHelper k8sTaskHelper;
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient ContainerService containerService;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    logger.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);
    ContainerInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), ContainerInstanceSyncPerpetualTaskParams.class);

    return K8S.name().equals(taskParams.getContainerType())
        ? executeK8sContainerSyncTask(taskId, taskParams.getK8SContainerPerpetualTaskParams())
        : executeContainerServiceSyncTask(taskId, taskParams.getContainerServicePerpetualTaskParams());
  }

  private PerpetualTaskResponse executeContainerServiceSyncTask(
      PerpetualTaskId taskId, ContainerServicePerpetualTaskParams containerServicePerpetualTaskParams) {
    final SettingAttribute settingAttribute =
        (SettingAttribute) KryoUtils.asObject(containerServicePerpetualTaskParams.getSettingAttribute().toByteArray());

    ContainerSyncResponse responseData =
        getContainerSyncResponse(containerServicePerpetualTaskParams, settingAttribute);
    publishInstanceSyncResult(
        taskId, settingAttribute.getAccountId(), containerServicePerpetualTaskParams.getNamespace(), responseData);

    boolean isFailureResponse = FAILURE == responseData.getCommandExecutionStatus();
    return PerpetualTaskResponse.builder()
        .responseCode(Response.SC_OK)
        .perpetualTaskState(isFailureResponse ? TASK_RUN_FAILED : TASK_RUN_SUCCEEDED)
        .responseMessage(isFailureResponse ? responseData.getErrorMessage() : TASK_RUN_SUCCEEDED.name())
        .build();
  }

  private ContainerSyncResponse getContainerSyncResponse(
      ContainerServicePerpetualTaskParams containerServicePerpetualTaskParams, SettingAttribute settingAttribute) {
    @SuppressWarnings("unchecked")
    final List<EncryptedDataDetail> encryptedDataDetails = (List<EncryptedDataDetail>) KryoUtils.asObject(
        containerServicePerpetualTaskParams.getEncryptionDetails().toByteArray());

    ContainerServiceParams request =
        ContainerServiceParams.builder()
            .settingAttribute(settingAttribute)
            .containerServiceName(containerServicePerpetualTaskParams.getContainerSvcName())
            .encryptionDetails(encryptedDataDetails)
            .clusterName(containerServicePerpetualTaskParams.getClusterName())
            .namespace(containerServicePerpetualTaskParams.getNamespace())
            .region(containerServicePerpetualTaskParams.getRegion())
            .subscriptionId(containerServicePerpetualTaskParams.getSubscriptionId())
            .resourceGroup(containerServicePerpetualTaskParams.getResourceGroup())
            .masterUrl(containerServicePerpetualTaskParams.getMasterUrl())
            .build();

    try {
      List<ContainerInfo> containerInfos = containerService.getContainerInfos(request);
      return ContainerSyncResponse.builder()
          .containerInfoList(containerInfos)
          .commandExecutionStatus((containerInfos != null) ? SUCCESS : FAILURE)
          .build();
    } catch (Exception exception) {
      logger.error(String.format("Failed to fetch containers info for namespace: [%s] and svc:[%s] ",
                       containerServicePerpetualTaskParams.getNamespace(),
                       containerServicePerpetualTaskParams.getContainerSvcName()),
          exception);

      return ContainerSyncResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage(exception.getMessage())
          .build();
    }
  }

  private PerpetualTaskResponse executeK8sContainerSyncTask(
      PerpetualTaskId taskId, K8sContainerInstanceSyncPerpetualTaskParams k8sContainerInstanceSyncPerpetualTaskParams) {
    final K8sClusterConfig k8sClusterConfig = (K8sClusterConfig) KryoUtils.asObject(
        k8sContainerInstanceSyncPerpetualTaskParams.getK8SClusterConfig().toByteArray());
    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig);

    K8sTaskExecutionResponse responseData =
        getK8sTaskResponse(k8sContainerInstanceSyncPerpetualTaskParams, kubernetesConfig);
    publishInstanceSyncResult(taskId, kubernetesConfig.getAccountId(),
        k8sContainerInstanceSyncPerpetualTaskParams.getNamespace(), responseData);

    boolean isFailureResponse = FAILURE == responseData.getCommandExecutionStatus();
    return PerpetualTaskResponse.builder()
        .responseCode(Response.SC_OK)
        .perpetualTaskState(isFailureResponse ? TASK_RUN_FAILED : TASK_RUN_SUCCEEDED)
        .responseMessage(isFailureResponse ? responseData.getErrorMessage() : TASK_RUN_SUCCEEDED.name())
        .build();
  }

  private K8sTaskExecutionResponse getK8sTaskResponse(
      K8sContainerInstanceSyncPerpetualTaskParams k8sContainerInstanceSyncPerpetualTaskParams,
      KubernetesConfig kubernetesConfig) {
    try {
      List<K8sPod> k8sPodList =
          k8sTaskHelper.getPodDetails(kubernetesConfig, k8sContainerInstanceSyncPerpetualTaskParams.getNamespace(),
              k8sContainerInstanceSyncPerpetualTaskParams.getReleaseName());

      return K8sTaskExecutionResponse.builder()
          .k8sTaskResponse(K8sInstanceSyncResponse.builder().k8sPodInfoList(k8sPodList).build())
          .commandExecutionStatus((k8sPodList != null) ? SUCCESS : FAILURE)
          .build();

    } catch (Exception exception) {
      logger.error(String.format("Failed to fetch k8s pod list for namespace: [%s] and releaseName:[%s] ",
                       k8sContainerInstanceSyncPerpetualTaskParams.getNamespace(),
                       k8sContainerInstanceSyncPerpetualTaskParams.getReleaseName()),
          exception);
      return K8sTaskExecutionResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage(exception.getMessage())
          .build();
    }
  }

  private void publishInstanceSyncResult(
      PerpetualTaskId taskId, String accountId, String namespace, ResponseData responseData) {
    try {
      execute(delegateAgentManagerClient.publishInstanceSyncResult(taskId.getId(), accountId, responseData));
    } catch (Exception e) {
      logger.error(
          String.format("Failed to publish container instance sync result. namespace [%s] and PerpetualTaskId [%s]",
              namespace, taskId.getId()),
          e);
    }
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
