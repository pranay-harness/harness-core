package io.harness.perpetualtask.ecs;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsEcsRequest;
import software.wings.service.impl.aws.model.AwsEcsRequest.AwsEcsRequestType;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EcsPerpetualTaskServiceClient implements PerpetualTaskServiceClient<EcsPerpetualTaskClientParams> {
  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private PerpetualTaskService perpetualTaskService;

  private static final String REGION = "region";
  private static final String SETTING_ID = "settingId";
  private static final String CLUSTER_NAME = "clusterName";
  private static final String CLUSTER_ID = "clusterId";

  @Override
  public String create(String accountId, EcsPerpetualTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(REGION, clientParams.getRegion());
    clientParamMap.put(SETTING_ID, clientParams.getSettingId());
    clientParamMap.put(CLUSTER_NAME, clientParams.getClusterName());
    clientParamMap.put(CLUSTER_ID, clientParams.getClusterId());

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(600))
                                         .setTimeout(Durations.fromMillis(180000))
                                         .build();

    return perpetualTaskService.createTask(PerpetualTaskType.ECS_CLUSTER, accountId, clientContext, schedule, false);
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
  public EcsPerpetualTaskParams getTaskParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    logger.info("Inside get task params for ecs perpetual task {} ", clientParams);
    String region = clientParams.get(REGION);
    String settingId = clientParams.get(SETTING_ID);
    String clusterName = clientParams.get(CLUSTER_NAME);
    String clusterId = clientParams.get(CLUSTER_ID);

    AwsConfig awsConfig = getAwsConfig(settingId);
    ByteString awsConfigBytes = ByteString.copyFrom(KryoUtils.asBytes(awsConfig));
    ByteString encryptionDetailBytes = ByteString.copyFrom(KryoUtils.asBytes(getEncryptionDetails(awsConfig)));

    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterName(clusterName)
                                                        .setClusterId(clusterId)
                                                        .setSettingId(settingId)
                                                        .setRegion(region)
                                                        .setEncryptionDetail(encryptionDetailBytes)
                                                        .setAwsConfig(awsConfigBytes)
                                                        .build();
    logger.debug("Get Task params {} ", ecsPerpetualTaskParams.toString());
    return ecsPerpetualTaskParams;
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    logger.debug("Nothing to do !!");
  }

  private AwsConfig getAwsConfig(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsConfig)) {
      throw new InvalidRequestException("AWS account setting not found " + settingId);
    }
    return (AwsConfig) settingAttribute.getValue();
  }

  private List<EncryptedDataDetail> getEncryptionDetails(AwsConfig awsConfig) {
    return secretManager.getEncryptionDetails(awsConfig, null, null);
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext context, String accountId) {
    Map<String, String> clientParams = context.getClientParams();
    String settingId = clientParams.get(SETTING_ID);
    AwsConfig awsConfig = getAwsConfig(settingId);
    String region = clientParams.get(REGION);

    AwsEcsRequest awsEcsRequest =
        new AwsEcsRequest(awsConfig, getEncryptionDetails(awsConfig), AwsEcsRequestType.LIST_CLUSTERS, region);

    return DelegateTask.builder()
        .accountId(accountId)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.AWS_ECS_TASK.name())
                  .parameters(new Object[] {awsEcsRequest})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .tags(isNotEmpty(awsConfig.getTag()) ? singletonList(awsConfig.getTag()) : null)
        .build();
  }
}
