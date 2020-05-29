package io.harness.perpetualtask.instancesync;

import static java.util.Objects.nonNull;
import static software.wings.service.InstanceSyncConstants.CONTAINER_SERVICE_NAME;
import static software.wings.service.InstanceSyncConstants.CONTAINER_TYPE;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.NAMESPACE;
import static software.wings.service.InstanceSyncConstants.RELEASE_NAME;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;
import static software.wings.service.InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES;
import static software.wings.utils.Utils.emptyIfNull;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sInstanceSyncTaskParameters;
import software.wings.service.impl.ContainerMetadataType;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.states.k8s.K8sStateHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContainerInstanceSyncPerpetualTaskClient
    implements PerpetualTaskServiceClient<ContainerInstanceSyncPerpetualTaskClientParams> {
  static final boolean ALLOW_DUPLICATE = false;
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject transient AwsCommandHelper awsCommandHelper;
  @Inject transient K8sStateHelper k8sStateHelper;
  @Inject SecretManager secretManager;
  @Inject SettingsService settingsService;

  @Override
  public String create(String accountId, ContainerInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(HARNESS_APPLICATION_ID, clientParams.getAppId());
    clientParamMap.put(INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId());
    clientParamMap.put(NAMESPACE, clientParams.getNamespace());
    clientParamMap.put(RELEASE_NAME, clientParams.getReleaseName());
    clientParamMap.put(CONTAINER_SERVICE_NAME, clientParams.getContainerSvcName());
    clientParamMap.put(CONTAINER_TYPE, emptyIfNull(clientParams.getContainerType()));

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);
    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(
        PerpetualTaskType.CONTAINER_INSTANCE_SYNC, accountId, clientContext, schedule, ALLOW_DUPLICATE);
  }

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    final ContainerInstanceSyncPerpetualTaskData taskData = getPerpetualTaskData(clientContext);
    return isK8sContainerType(clientContext.getClientParams()) ? buildK8ContainerInstanceSyncTaskParams(taskData)
                                                               : buildContainerInstanceSyncTaskParams(taskData);
  }

  private Message buildContainerInstanceSyncTaskParams(ContainerInstanceSyncPerpetualTaskData taskData) {
    ByteString settingAttribute = ByteString.copyFrom(KryoUtils.asBytes(taskData.getSettingAttribute()));
    ByteString encryptionDetails = ByteString.copyFrom(KryoUtils.asBytes(taskData.getEncryptionDetails()));

    return ContainerInstanceSyncPerpetualTaskParams.newBuilder()
        .setContainerType(taskData.getContainerType())
        .setContainerServicePerpetualTaskParams(ContainerServicePerpetualTaskParams.newBuilder()
                                                    .setSettingAttribute(settingAttribute)
                                                    .setContainerSvcName(taskData.getContainerServiceName())
                                                    .setEncryptionDetails(encryptionDetails)
                                                    .setClusterName(taskData.getClusterName())
                                                    .setNamespace(taskData.getNamespace())
                                                    .setRegion(taskData.getRegion())
                                                    .setSubscriptionId(taskData.getSubscriptionId())
                                                    .setResourceGroup(taskData.getResourceGroup())
                                                    .setMasterUrl(taskData.getMasterUrl())
                                                    .build())
        .build();
  }

  private Message buildK8ContainerInstanceSyncTaskParams(ContainerInstanceSyncPerpetualTaskData taskData) {
    ByteString clusterConfig = ByteString.copyFrom(KryoUtils.asBytes(taskData.getK8sClusterConfig()));

    return ContainerInstanceSyncPerpetualTaskParams.newBuilder()
        .setContainerType(taskData.getContainerType())
        .setK8SContainerPerpetualTaskParams(K8sContainerInstanceSyncPerpetualTaskParams.newBuilder()
                                                .setAccountId(taskData.getAccountId())
                                                .setAppId(taskData.getAppId())
                                                .setK8SClusterConfig(clusterConfig)
                                                .setNamespace(taskData.getNamespace())
                                                .setReleaseName(taskData.getReleaseName())
                                                .build())
        .build();
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    // Instance Sync Perpetual Task Framework takes care of this via Perpetual Task Response
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    Map<String, String> clientParams = clientContext.getClientParams();
    final ContainerInstanceSyncPerpetualTaskData taskData = getPerpetualTaskData(clientContext);

    return isK8sContainerType(clientParams) ? buildK8sDelegateTask(clientParams, taskData)
                                            : buildNonK8sDelegateTask(clientParams, taskData);
  }

  private DelegateTask buildNonK8sDelegateTask(
      Map<String, String> clientParams, ContainerInstanceSyncPerpetualTaskData taskData) {
    ContainerServiceParams delegateTaskParams = ContainerServiceParams.builder()
                                                    .settingAttribute(taskData.getSettingAttribute())
                                                    .containerServiceName(taskData.getContainerServiceName())
                                                    .encryptionDetails(taskData.getEncryptionDetails())
                                                    .clusterName(taskData.getClusterName())
                                                    .namespace(taskData.getNamespace())
                                                    .region(taskData.getRegion())
                                                    .subscriptionId(taskData.getSubscriptionId())
                                                    .resourceGroup(taskData.getResourceGroup())
                                                    .masterUrl(taskData.getMasterUrl())
                                                    .build();

    return DelegateTask.builder()
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.CONTAINER_INFO.name())
                  .parameters(new Object[] {delegateTaskParams})
                  .timeout(TimeUnit.MINUTES.toMillis(VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .accountId(taskData.getAccountId())
        .appId(taskData.getAppId())
        .envId(taskData.getEnvId())
        .infrastructureMappingId(clientParams.get(INFRASTRUCTURE_MAPPING_ID))
        .tags(k8sStateHelper.fetchTagsFromK8sCloudProvider(delegateTaskParams))
        .build();
  }

  private DelegateTask buildK8sDelegateTask(
      Map<String, String> clientParams, ContainerInstanceSyncPerpetualTaskData taskData) {
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
        .appId(taskData.getAppId())
        .waitId(UUIDGenerator.generateUuid())
        .tags(ListUtils.union(k8sStateHelper.fetchTagsFromK8sTaskParams(delegateTaskParams),
            awsCommandHelper.getAwsConfigTagsFromK8sConfig(delegateTaskParams)))
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.K8S_COMMAND_TASK.name())
                  .parameters(new Object[] {delegateTaskParams})
                  .timeout(TimeUnit.MINUTES.toMillis(VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .envId(taskData.getEnvId())
        .infrastructureMappingId(infraMappingId)
        .build();
  }

  private ContainerInstanceSyncPerpetualTaskData getPerpetualTaskData(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String appId = clientParams.get(HARNESS_APPLICATION_ID);
    String infraMappingId = clientParams.get(INFRASTRUCTURE_MAPPING_ID);
    ContainerInfrastructureMapping containerInfrastructureMapping =
        (ContainerInfrastructureMapping) infraMappingService.get(appId, infraMappingId);

    boolean isK8sContainerType = isK8sContainerType(clientParams);
    SettingAttribute settingAttribute = getSettingAttribute(isK8sContainerType, containerInfrastructureMapping);

    return ContainerInstanceSyncPerpetualTaskData.builder()
        .containerType(emptyIfNull(clientParams.get(CONTAINER_TYPE)))
        .accountId(containerInfrastructureMapping.getAccountId())
        .appId(containerInfrastructureMapping.getAppId())
        .namespace(emptyIfNull(clientParams.get(NAMESPACE)))
        .releaseName(emptyIfNull(clientParams.get(RELEASE_NAME)))
        .containerServiceName(emptyIfNull(clientParams.get(CONTAINER_SERVICE_NAME)))
        .k8sClusterConfig(getK8sClusterConfig(isK8sContainerType, containerInfrastructureMapping))
        .clusterName(emptyIfNull(getClusterName(isK8sContainerType, containerInfrastructureMapping)))
        .settingAttribute(settingAttribute)
        .encryptionDetails(getEncryptedDataDetails(settingAttribute, containerInfrastructureMapping))
        .region(emptyIfNull(getRegion(containerInfrastructureMapping)))
        .subscriptionId(emptyIfNull(getSubscriptionId(containerInfrastructureMapping)))
        .resourceGroup(emptyIfNull(getResourceGroup(containerInfrastructureMapping)))
        .masterUrl(emptyIfNull(getMasterUrl(containerInfrastructureMapping)))
        .envId(emptyIfNull(containerInfrastructureMapping.getEnvId()))
        .build();
  }

  private boolean isK8sContainerType(Map<String, String> clientParams) {
    String containerType = clientParams.get(CONTAINER_TYPE);
    return ContainerMetadataType.K8S.name().equals(containerType);
  }

  private SettingAttribute getSettingAttribute(
      boolean isK8sContainerType, ContainerInfrastructureMapping containerInfrastructureMapping) {
    return !isK8sContainerType ? settingsService.get(containerInfrastructureMapping.getComputeProviderSettingId())
                               : null;
  }
  private K8sClusterConfig getK8sClusterConfig(
      boolean isK8sContainerType, ContainerInfrastructureMapping containerInfrastructureMapping) {
    return isK8sContainerType ? containerDeploymentManagerHelper.getK8sClusterConfig(containerInfrastructureMapping)
                              : null;
  }

  private List<EncryptedDataDetail> getEncryptedDataDetails(
      SettingAttribute settingAttribute, ContainerInfrastructureMapping containerInfraMapping) {
    return nonNull(settingAttribute)
        ? secretManager.getEncryptionDetails(
              (EncryptableSetting) settingAttribute.getValue(), containerInfraMapping.getAppId(), null)
        : null;
  }

  private String getClusterName(
      boolean isK8sContainerType, ContainerInfrastructureMapping containerInfrastructureMapping) {
    return !isK8sContainerType ? containerInfrastructureMapping.getClusterName() : null;
  }

  private String getRegion(ContainerInfrastructureMapping containerInfrastructureMapping) {
    return (containerInfrastructureMapping instanceof EcsInfrastructureMapping)
        ? ((EcsInfrastructureMapping) containerInfrastructureMapping).getRegion()
        : null;
  }

  private String getSubscriptionId(ContainerInfrastructureMapping containerInfrastructureMapping) {
    return containerInfrastructureMapping instanceof AzureKubernetesInfrastructureMapping
        ? ((AzureKubernetesInfrastructureMapping) containerInfrastructureMapping).getSubscriptionId()
        : null;
  }

  private String getResourceGroup(ContainerInfrastructureMapping containerInfrastructureMapping) {
    return containerInfrastructureMapping instanceof AzureKubernetesInfrastructureMapping
        ? ((AzureKubernetesInfrastructureMapping) containerInfrastructureMapping).getResourceGroup()
        : null;
  }

  private String getMasterUrl(ContainerInfrastructureMapping containerInfrastructureMapping) {
    return containerInfrastructureMapping instanceof AzureKubernetesInfrastructureMapping
        ? ((AzureKubernetesInfrastructureMapping) containerInfrastructureMapping).getMasterUrl()
        : null;
  }

  @Data
  @Builder
  static class ContainerInstanceSyncPerpetualTaskData {
    String containerType;
    String accountId;
    String appId;
    String namespace;
    String releaseName;
    String containerServiceName;
    String envId;
    K8sClusterConfig k8sClusterConfig;
    SettingAttribute settingAttribute;
    List<EncryptedDataDetail> encryptionDetails;
    String clusterName;
    String region;
    String subscriptionId;
    String resourceGroup;
    String masterUrl;
  }
}
