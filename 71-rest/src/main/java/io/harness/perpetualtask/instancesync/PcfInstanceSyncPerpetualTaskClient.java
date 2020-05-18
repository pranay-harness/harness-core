package io.harness.perpetualtask.instancesync;

import static io.harness.perpetualtask.PerpetualTaskType.PCF_INSTANCE_SYNC;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.InstanceSyncConstants.HARNESS_ACCOUNT_ID;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import com.google.common.annotations.VisibleForTesting;
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
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.service.impl.instance.PcfInstanceSyncPTDelegateParams;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PcfInstanceSyncPerpetualTaskClient
    implements PerpetualTaskServiceClient<PcfInstanceSyncPerpetualTaskClientParams> {
  public static final String PCF_APPLICATION_NAME = "pcfApplicationName";

  @Inject PerpetualTaskService perpetualTaskService;
  @Inject SecretManager secretManager;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;

  @Override
  public String create(String accountId, PcfInstanceSyncPerpetualTaskClientParams clientParams) {
    Map<String, String> paramMap = new HashMap<>();
    paramMap.put(HARNESS_ACCOUNT_ID, clientParams.getAccountId());
    paramMap.put(INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId());
    paramMap.put(HARNESS_APPLICATION_ID, clientParams.getAppId());
    paramMap.put(PCF_APPLICATION_NAME, clientParams.getApplicationName());

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(paramMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(PCF_INSTANCE_SYNC, accountId, clientContext, schedule, false);
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
    PcfInstanceSyncPTDelegateParams perpetualTaskParams = getPcfInstanceSyncPTDelegateParams(clientContext);

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(perpetualTaskParams.getPcfConfig(), null, null);

    ByteString configBytes = ByteString.copyFrom(KryoUtils.asBytes(perpetualTaskParams.getPcfConfig()));
    ByteString encryptedConfigBytes = ByteString.copyFrom(KryoUtils.asBytes(encryptionDetails));

    return PcfInstanceSyncPerpetualTaskParams.newBuilder()
        .setApplicationName(perpetualTaskParams.getApplicationName())
        .setInfraMappingId(perpetualTaskParams.getInfraMappingId())
        .setOrgName(perpetualTaskParams.getOrgName())
        .setPcfConfig(configBytes)
        .setEncryptedData(encryptedConfigBytes)
        .setSpace(perpetualTaskParams.getSpace())
        .build();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    PcfInstanceSyncPTDelegateParams perpetualTaskParams = getPcfInstanceSyncPTDelegateParams(clientContext);
    PcfConfig pcfConfig = perpetualTaskParams.getPcfConfig();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(pcfConfig, null, null);
    return buildDelegateTask(perpetualTaskParams, pcfConfig, encryptionDetails);
  }

  private DelegateTask buildDelegateTask(PcfInstanceSyncPTDelegateParams perpetualTaskParams, PcfConfig pcfConfig,
      List<EncryptedDataDetail> encryptionDetails) {
    return DelegateTask.builder()
        .accountId(pcfConfig.getAccountId())
        .appId(GLOBAL_APP_ID)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.PCF_COMMAND_TASK.name())
                  .parameters(new Object[] {PcfInstanceSyncRequest.builder()
                                                .pcfConfig(pcfConfig)
                                                .pcfApplicationName(perpetualTaskParams.getApplicationName())
                                                .organization(perpetualTaskParams.getOrgName())
                                                .space(perpetualTaskParams.getSpace())
                                                .pcfCommandType(PcfCommandRequest.PcfCommandType.APP_DETAILS)
                                                .timeoutIntervalInMin((int) TimeUnit.SECONDS.toMinutes(TIMEOUT_SECONDS))
                                                .build(),
                      encryptionDetails})
                  .timeout(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
                  .build())
        .build();
  }

  @VisibleForTesting
  PcfInstanceSyncPTDelegateParams getPcfInstanceSyncPTDelegateParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    return getPerpetualTaskParams(clientParams.get(INFRASTRUCTURE_MAPPING_ID), clientParams.get(PCF_APPLICATION_NAME),
        clientParams.get(HARNESS_APPLICATION_ID));
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    // Instance Sync Perpetual Task Framework takes care of this via Perpetual Task Response
  }

  private PcfInstanceSyncPTDelegateParams getPerpetualTaskParamsInternal(
      PcfInfrastructureMapping pcfInfrastructureMapping, String applicationName) {
    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();
    return PcfInstanceSyncPTDelegateParams.builder()
        .applicationName(applicationName)
        .orgName(pcfInfrastructureMapping.getOrganization())
        .space(pcfInfrastructureMapping.getSpace())
        .pcfConfig(pcfConfig)
        .infraMappingId(pcfInfrastructureMapping.getUuid())
        .build();
  }

  private PcfInstanceSyncPTDelegateParams getPerpetualTaskParams(
      String infraMappingId, String applicationName, String appId) {
    PcfInfrastructureMapping pcfInfrastructureMapping =
        (PcfInfrastructureMapping) infraMappingService.get(appId, infraMappingId);

    return getPerpetualTaskParamsInternal(pcfInfrastructureMapping, applicationName);
  }
}
