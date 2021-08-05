package io.harness.perpetualtask.instancesync;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.DelegateTask.DELEGATE_QUEUE_TIMEOUT;

import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.NAMESPACE;
import static software.wings.service.InstanceSyncConstants.RELEASE_NAME;
import static software.wings.service.InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES;
import static software.wings.utils.Utils.emptyIfNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sInstanceSyncTaskParameters;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CDP)
public class K8sInstanceSyncPerpetualTaskClient implements PerpetualTaskServiceClient {
  @Inject InfrastructureMappingService infraMappingService;
  @Inject EnvironmentService environmentService;
  @Inject ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject KryoSerializer kryoSerializer;

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    final K8sInstanceSyncPerpetualTaskData taskData = getPerpetualTaskData(clientContext);
    return buildK8ContainerInstanceSyncTaskParams(taskData);
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    Map<String, String> clientParams = clientContext.getClientParams();
    final K8sInstanceSyncPerpetualTaskData taskData = getPerpetualTaskData(clientContext);
    return buildK8sDelegateTask(clientParams, taskData);
  }

  private K8sInstanceSyncPerpetualTaskData getPerpetualTaskData(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String appId = clientParams.get(HARNESS_APPLICATION_ID);
    String infraMappingId = clientParams.get(INFRASTRUCTURE_MAPPING_ID);
    AzureKubernetesInfrastructureMapping kubernetesInfrastructureMapping =
        (AzureKubernetesInfrastructureMapping) infraMappingService.get(appId, infraMappingId);

    Environment environment = environmentService.get(appId, kubernetesInfrastructureMapping.getEnvId());
    K8sClusterConfig k8sClusterConfig =
        containerDeploymentManagerHelper.getK8sClusterConfig(kubernetesInfrastructureMapping, null);

    return K8sInstanceSyncPerpetualTaskData.builder()
        .accountId(kubernetesInfrastructureMapping.getAccountId())
        .appId(kubernetesInfrastructureMapping.getAppId())
        .namespace(emptyIfNull(clientParams.get(NAMESPACE)))
        .releaseName(emptyIfNull(clientParams.get(RELEASE_NAME)))
        .k8sClusterConfig(k8sClusterConfig)
        .envId(emptyIfNull(environment.getUuid()))
        .envType(environment.getEnvironmentType().name())
        .serviceId(kubernetesInfrastructureMapping.getServiceId())
        .build();
  }

  private Message buildK8ContainerInstanceSyncTaskParams(K8sInstanceSyncPerpetualTaskData taskData) {
    ByteString clusterConfig = ByteString.copyFrom(kryoSerializer.asBytes(taskData.getK8sClusterConfig()));

    return K8sContainerInstanceSyncPerpetualTaskParams.newBuilder()
        .setAccountId(taskData.getAccountId())
        .setAppId(taskData.getAppId())
        .setK8SClusterConfig(clusterConfig)
        .setNamespace(taskData.getNamespace())
        .setReleaseName(taskData.getReleaseName())
        .build();
  }

  private DelegateTask buildK8sDelegateTask(
      Map<String, String> clientParams, K8sInstanceSyncPerpetualTaskData taskData) {
    String infraMappingId = clientParams.get(INFRASTRUCTURE_MAPPING_ID);

    K8sInstanceSyncTaskParameters delegateTaskParams = K8sInstanceSyncTaskParameters.builder()
                                                           .accountId(taskData.getAccountId())
                                                           .appId(taskData.getAppId())
                                                           .k8sClusterConfig(taskData.getK8sClusterConfig())
                                                           .namespace(taskData.getNamespace())
                                                           .releaseName(taskData.getReleaseName())
                                                           .build();

    return DelegateTask.builder()
        .accountId(taskData.getAccountId())
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, taskData.getAppId())
        .waitId(UUIDGenerator.generateUuid())
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.K8S_COMMAND_TASK.name())
                  .parameters(new Object[] {delegateTaskParams})
                  .timeout(TimeUnit.MINUTES.toMillis(VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, taskData.getEnvId())
        .setupAbstraction(Cd1SetupFields.ENV_TYPE_FIELD, taskData.getEnvType())
        .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, infraMappingId)
        .setupAbstraction(Cd1SetupFields.SERVICE_ID_FIELD, taskData.getServiceId())
        .expiry(System.currentTimeMillis() + DELEGATE_QUEUE_TIMEOUT)
        .build();
  }

  @Data
  @Builder
  static class K8sInstanceSyncPerpetualTaskData {
    String accountId;
    String appId;
    String namespace;
    String releaseName;
    K8sClusterConfig k8sClusterConfig;
    String serviceId;
    String envId;
    String envType;
  }
}
