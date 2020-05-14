package io.harness.perpetualtask.instancesync;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import com.amazonaws.services.ec2.model.Filter;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.perpetualtask.AwsSshPTClientParams;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.AwsSshInstanceSyncPTParamsOuterClass.AwsSshInstanceSyncPTParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesRequest;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AwsSshPerpetualTaskServiceClient implements PerpetualTaskServiceClient<AwsSshPTClientParams> {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private SecretManager secretManager;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AwsUtils awsUtils;
  @Inject private SettingsService settingsService;

  @Override
  public String create(String accountId, AwsSshPTClientParams clientParams) {
    Map<String, String> clientParamMap = ImmutableMap.of(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID,
        clientParams.getInframappingId(), InstanceSyncConstants.HARNESS_APPLICATION_ID, clientParams.getAppId());
    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                                         .build();
    return perpetualTaskService.createTask(
        PerpetualTaskType.AWS_SSH_INSTANCE_SYNC, accountId, clientContext, schedule, false);
  }

  @Override
  public boolean reset(String accountId, String taskId) {
    return perpetualTaskService.resetTask(accountId, taskId);
  }

  @Override
  public boolean delete(String accountId, String taskId) {
    return perpetualTaskService.deleteTask(accountId, taskId);
  }

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    final PerpetualTaskData taskData = getPerpetualTaskData(clientContext);

    ByteString configBytes = ByteString.copyFrom(KryoUtils.asBytes(taskData.getAwsConfig()));
    ByteString filterBytes = ByteString.copyFrom(KryoUtils.asBytes(taskData.getFilters()));
    ByteString encryptionDetailsBytes = ByteString.copyFrom(KryoUtils.asBytes(taskData.getEncryptionDetails()));

    return AwsSshInstanceSyncPTParams.newBuilder()
        .setRegion(taskData.getRegion())
        .setAwsConfig(configBytes)
        .setFilter(filterBytes)
        .setEncryptedData(encryptionDetailsBytes)
        .build();
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    // Instance Sync Perpetual Task Framework takes care of this via Perpetual Task Response
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    final PerpetualTaskData taskData = getPerpetualTaskData(clientContext);

    return DelegateTask.builder()
        .accountId(accountId)
        .appId(GLOBAL_APP_ID)
        .tags(isNotEmpty(taskData.getAwsConfig().getTag()) ? singletonList(taskData.getAwsConfig().getTag()) : null)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.AWS_EC2_TASK.name())
                  .parameters(new Object[] {AwsEc2ListInstancesRequest.builder()
                                                .awsConfig(taskData.getAwsConfig())
                                                .encryptionDetails(taskData.getEncryptionDetails())
                                                .region(taskData.getRegion())
                                                .filters(taskData.getFilters())
                                                .build()})
                  .timeout(TimeUnit.MINUTES.toMillis(InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .build();
  }

  private PerpetualTaskData getPerpetualTaskData(PerpetualTaskClientContext clientContext) {
    String infraMappingId = getInfraMappingId(clientContext);
    String appId = getAppId(clientContext);

    InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (!(infraMapping instanceof AwsInfrastructureMapping)) {
      String msg = "Incompatible infra mapping type. Expecting AwsInfraMapping type. Found:"
          + infraMapping.getInfraMappingType();
      logger.error(msg);
      throw new InvalidRequestException(msg);
    }

    DeploymentType deploymentType =
        serviceResourceService.getDeploymentType(infraMapping, null, infraMapping.getServiceId());

    List<Filter> filters = awsUtils.getAwsFilters((AwsInfrastructureMapping) infraMapping, deploymentType);
    SettingAttribute awsCloudProvider = settingsService.get(infraMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) awsCloudProvider.getValue();

    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig);

    return PerpetualTaskData.builder()
        .region(((AwsInfrastructureMapping) infraMapping).getRegion())
        .awsConfig(awsConfig)
        .filters(filters)
        .encryptionDetails(encryptionDetails)
        .build();
  }

  private String getAppId(PerpetualTaskClientContext clientContext) {
    return clientContext.getClientParams().get(InstanceSyncConstants.HARNESS_APPLICATION_ID);
  }

  private String getInfraMappingId(PerpetualTaskClientContext clientContext) {
    return clientContext.getClientParams().get(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID);
  }

  @Data
  @Builder
  private static class PerpetualTaskData {
    private String region;
    private AwsConfig awsConfig;
    private List<Filter> filters;
    private List<EncryptedDataDetail> encryptionDetails;
  }
}
