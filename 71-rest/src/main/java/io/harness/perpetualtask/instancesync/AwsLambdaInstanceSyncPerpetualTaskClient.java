package io.harness.perpetualtask.instancesync;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.Boolean.TRUE;
import static java.lang.Long.parseLong;
import static java.util.Collections.singletonList;
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
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsLambdaInstanceSyncPerpetualTaskClient
    implements PerpetualTaskServiceClient<AwsLambdaInstanceSyncPerpetualTaskClientParams> {
  public static final String FUNCTION_NAME = "functionName";
  public static final String QUALIFIER = "qualifier";
  public static final String START_DATE = "startDate";

  @Inject PerpetualTaskService perpetualTaskService;
  @Inject SecretManager secretManager;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;

  @Override
  public String create(String accountId, AwsLambdaInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> paramMap = ImmutableMap.<String, String>builder()
                                       .put(HARNESS_APPLICATION_ID, clientParams.getAppId())
                                       .put(INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId())
                                       .put(FUNCTION_NAME, clientParams.getFunctionName())
                                       .put(QUALIFIER, clientParams.getQualifier())
                                       .put(START_DATE, clientParams.getStartDate())
                                       .build();

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(paramMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(
        PerpetualTaskType.AWS_LAMBDA_INSTANCE_SYNC, accountId, clientContext, schedule, false);
  }

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    final PerpetualTaskData perpetualTaskData = getPerpetualTaskData(clientContext);

    ByteString awsConfigBytes = ByteString.copyFrom(KryoUtils.asBytes(perpetualTaskData.getAwsConfig()));
    ByteString encryptedAwsConfigBytes =
        ByteString.copyFrom(KryoUtils.asBytes(perpetualTaskData.getEncryptedDataDetails()));

    return AwsLambdaInstanceSyncPerpetualTaskParams.newBuilder()
        .setAwsConfig(awsConfigBytes)
        .setEncryptedData(encryptedAwsConfigBytes)
        .setRegion(perpetualTaskData.getRegion())
        .setFunctionName(perpetualTaskData.getFunctionName())
        .setQualifier(perpetualTaskData.getQualifier())
        .setStartDate(perpetualTaskData.getStartDate())
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
    AwsLambdaDetailsRequest request = AwsLambdaDetailsRequest.builder()
                                          .awsConfig(perpetualTaskData.getAwsConfig())
                                          .encryptionDetails(perpetualTaskData.getEncryptedDataDetails())
                                          .region(perpetualTaskData.getRegion())
                                          .functionName(perpetualTaskData.getFunctionName())
                                          .qualifier(perpetualTaskData.getQualifier())
                                          .loadAliases(TRUE)
                                          .build();

    return DelegateTask.builder()
        .accountId(accountId)
        .tags(isNotEmpty(perpetualTaskData.getAwsConfig().getTag())
                ? singletonList(perpetualTaskData.getAwsConfig().getTag())
                : null)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.AWS_LAMBDA_TASK.name())
                  .parameters(new Object[] {request})
                  .timeout(TimeUnit.MINUTES.toMillis(InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .build();
  }

  private PerpetualTaskData getPerpetualTaskData(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String appId = clientParams.get(HARNESS_APPLICATION_ID);
    String infraMappingId = clientParams.get(INFRASTRUCTURE_MAPPING_ID);
    AwsLambdaInfraStructureMapping infraMapping =
        (AwsLambdaInfraStructureMapping) infraMappingService.get(appId, infraMappingId);

    SettingAttribute cloudProviderSetting = settingsService.get(infraMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, null, null);

    return PerpetualTaskData.builder()
        .awsConfig(awsConfig)
        .encryptedDataDetails(encryptionDetails)
        .region(infraMapping.getRegion())
        .functionName(clientContext.getClientParams().get(FUNCTION_NAME))
        .qualifier(clientContext.getClientParams().get(QUALIFIER))
        .startDate(parseLong(clientContext.getClientParams().get(START_DATE)))
        .build();
  }

  @Data
  @Builder
  static class PerpetualTaskData {
    AwsConfig awsConfig;
    List<EncryptedDataDetail> encryptedDataDetails;
    String region;
    String functionName;
    String qualifier;
    long startDate;
  }
}
