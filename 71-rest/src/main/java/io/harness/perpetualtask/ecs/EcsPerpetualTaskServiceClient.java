package io.harness.perpetualtask.ecs;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.exception.InvalidRequestException;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EcsPerpetualTaskServiceClient implements PerpetualTaskServiceClient<EcsPerpetualTaskClientParams> {
  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private PerpetualTaskService perpetualTaskService;

  private static final String REGION = "region";
  private static final String SETTING_ID = "settingId";
  private static final String CLUSTER_NAME = "clusterName";

  @Override
  public String create(String accountId, EcsPerpetualTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(REGION, clientParams.getRegion());
    clientParamMap.put(SETTING_ID, clientParams.getSettingId());
    clientParamMap.put(CLUSTER_NAME, clientParams.getClusterName());
    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(600))
                                         .setTimeout(Durations.fromMillis(180000))
                                         .build();

    return perpetualTaskService.createTask(PerpetualTaskType.ECS_CLUSTER, accountId, clientContext, schedule, false);
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

    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsConfig)) {
      throw new InvalidRequestException("AWS account setting not found " + settingId);
    }
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    ByteString awsConfigBytes = ByteString.copyFrom(KryoUtils.asBytes(awsConfig));
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, null, null);
    ByteString encryptionDetailBytes = ByteString.copyFrom(KryoUtils.asBytes(encryptionDetails));

    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterName(clusterName)
                                                        .setRegion(region)
                                                        .setEncryptionDetail(encryptionDetailBytes)
                                                        .setAwsConfig(awsConfigBytes)
                                                        .build();
    logger.debug("Get Task params {} ", ecsPerpetualTaskParams.toString());
    return ecsPerpetualTaskParams;
  }
}
