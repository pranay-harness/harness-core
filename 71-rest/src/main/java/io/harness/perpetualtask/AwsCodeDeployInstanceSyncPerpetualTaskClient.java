package io.harness.perpetualtask;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.perpetualtask.PerpetualTaskType.AWS_CODE_DEPLOY_INSTANCE_SYNC;
import static java.util.Collections.singletonList;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.TaskType.AWS_EC2_TASK;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import com.amazonaws.services.ec2.model.Filter;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.Data;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesRequest;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AwsCodeDeployInstanceSyncPerpetualTaskClient
    implements PerpetualTaskServiceClient<AwsCodeDeployInstanceSyncPerpetualTaskClientParams> {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AwsUtils awsUtils;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;

  @Override
  public String create(String accountId, AwsCodeDeployInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> paramsMap = new HashMap<>();
    paramsMap.put(INFRASTRUCTURE_MAPPING_ID, clientParams.getInframmapingId());
    paramsMap.put(HARNESS_APPLICATION_ID, clientParams.getAppId());

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(paramsMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(AWS_CODE_DEPLOY_INSTANCE_SYNC, accountId, clientContext, schedule, false);
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
    PerpetualTaskData taskData = getPerpetualTaskData(clientContext);
    ByteString filterBytes = ByteString.copyFrom(KryoUtils.asBytes(taskData.getFilters()));
    ByteString configBytes = ByteString.copyFrom(KryoUtils.asBytes(taskData.getAwsConfig()));
    ByteString encryptionDetailsBytes = ByteString.copyFrom(KryoUtils.asBytes(taskData.getEncryptionDetails()));

    return AwsCodeDeployInstanceSyncPerpetualTaskParams.newBuilder()
        .setRegion(taskData.getRegion())
        .setFilter(filterBytes)
        .setAwsConfig(configBytes)
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
    PerpetualTaskData taskData = getPerpetualTaskData(clientContext);
    AwsEc2ListInstancesRequest request = AwsEc2ListInstancesRequest.builder()
                                             .awsConfig(taskData.getAwsConfig())
                                             .encryptionDetails(taskData.getEncryptionDetails())
                                             .region(taskData.getRegion())
                                             .filters(taskData.getFilters())
                                             .build();

    return DelegateTask.builder()
        .accountId(accountId)
        .appId(GLOBAL_APP_ID)
        .tags(isNotEmpty(taskData.getAwsConfig().getTag()) ? singletonList(taskData.getAwsConfig().getTag()) : null)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(AWS_EC2_TASK.name())
                  .parameters(new Object[] {request})
                  .timeout(TimeUnit.MINUTES.toMillis(InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .build();
  }

  private PerpetualTaskData getPerpetualTaskData(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String infraMappingId = clientParams.get(INFRASTRUCTURE_MAPPING_ID);
    String appId = clientParams.get(HARNESS_APPLICATION_ID);
    CodeDeployInfrastructureMapping infraMapping =
        (CodeDeployInfrastructureMapping) infraMappingService.get(appId, infraMappingId);
    SettingAttribute awsCloudProvider = settingsService.get(infraMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) awsCloudProvider.getValue();
    DeploymentType deploymentType =
        serviceResourceService.getDeploymentType(infraMapping, null, infraMapping.getServiceId());
    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(awsConfig);

    return PerpetualTaskData.builder()
        .region(infraMapping.getRegion())
        .filters(getAwsFilters(infraMapping, deploymentType))
        .awsConfig(awsConfig)
        .encryptionDetails(encryptedDataDetails)
        .build();
  }

  private List<Filter> getAwsFilters(CodeDeployInfrastructureMapping infraMapping, DeploymentType deploymentType) {
    AwsInfrastructureMapping awsInfraMapping = AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping()
                                                   .withRegion(infraMapping.getRegion())
                                                   .withDeploymentType(deploymentType.name())
                                                   .withAwsInstanceFilter(null)
                                                   .build();

    return awsUtils.getAwsFilters(awsInfraMapping, deploymentType);
  }

  @Data
  @Builder
  private static class PerpetualTaskData {
    String region;
    AwsConfig awsConfig;
    String deploymentId;
    List<Filter> filters;
    List<EncryptedDataDetail> encryptionDetails;
  }
}
