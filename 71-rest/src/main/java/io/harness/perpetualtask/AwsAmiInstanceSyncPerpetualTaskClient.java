package io.harness.perpetualtask;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.instancesync.AwsAmiInstanceSyncPerpetualTaskParamsOuterClass.AwsAmiInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.aws.model.AwsAsgListInstancesRequest;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsAmiInstanceSyncPerpetualTaskClient
    implements PerpetualTaskServiceClient<AwsAmiInstanceSyncPerpetualTaskClientParams> {
  public static final String ASG_NAME = "asgName";
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject SecretManager secretManager;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;

  @Override
  public String create(String accountId, AwsAmiInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> paramMap = ImmutableMap.of(HARNESS_APPLICATION_ID, clientParams.getAppId(),
        INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId(), ASG_NAME, clientParams.getAsgName());

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(paramMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(
        PerpetualTaskType.AWS_AMI_INSTANCE_SYNC, accountId, clientContext, schedule, false);
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
    final PerpetualTaskData perpetualTaskData = getPerpetualTaskData(clientContext);

    ByteString configBytes = ByteString.copyFrom(KryoUtils.asBytes(perpetualTaskData.getAwsConfig()));
    ByteString encryptedConfigBytes =
        ByteString.copyFrom(KryoUtils.asBytes(perpetualTaskData.getEncryptedDataDetails()));

    return AwsAmiInstanceSyncPerpetualTaskParams.newBuilder()
        .setRegion(perpetualTaskData.getRegion())
        .setAsgName(perpetualTaskData.getAsgName())
        .setAwsConfig(configBytes)
        .setEncryptedData(encryptedConfigBytes)
        .build();
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    // Instance Sync Perpetual Task Framework takes care of this via Perpetual Task Response
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    final PerpetualTaskData perpetualTaskData = getPerpetualTaskData(clientContext);
    return DelegateTask.builder()
        .accountId(perpetualTaskData.getAwsConfig().getAccountId())
        .appId(GLOBAL_APP_ID)
        .tags(isNotEmpty(perpetualTaskData.getAwsConfig().getTag())
                ? singletonList(perpetualTaskData.getAwsConfig().getTag())
                : null)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.AWS_ASG_TASK.name())
                  .parameters(new Object[] {AwsAsgListInstancesRequest.builder()
                                                .awsConfig(perpetualTaskData.getAwsConfig())
                                                .encryptionDetails(perpetualTaskData.getEncryptedDataDetails())
                                                .region(perpetualTaskData.getRegion())
                                                .autoScalingGroupName(perpetualTaskData.getAsgName())
                                                .build()})
                  .timeout(TimeUnit.MINUTES.toMillis(InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .build();
  }

  private PerpetualTaskData getPerpetualTaskData(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String appId = clientParams.get(HARNESS_APPLICATION_ID);
    String infraMappingId = clientParams.get(INFRASTRUCTURE_MAPPING_ID);
    AwsAmiInfrastructureMapping infraMapping =
        (AwsAmiInfrastructureMapping) infraMappingService.get(appId, infraMappingId);

    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, null, null);

    return PerpetualTaskData.builder()
        .region(infraMapping.getRegion())
        .awsConfig(awsConfig)
        .asgName(clientContext.getClientParams().get(ASG_NAME))
        .encryptedDataDetails(encryptionDetails)
        .build();
  }

  @Data
  @Builder
  static class PerpetualTaskData {
    String region;
    AwsConfig awsConfig;
    String asgName;
    List<EncryptedDataDetail> encryptedDataDetails;
  }
}
