package io.harness.perpetualtask.instanceSync;

import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.InfrastructureMapping.ID_KEY;
import static software.wings.service.PcfInstanceSyncConstants.APPLICATION_ID;
import static software.wings.service.PcfInstanceSyncConstants.APPLICATION_NAME;
import static software.wings.service.PcfInstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.PcfInstanceSyncConstants.TIMEOUT_SECONDS;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instanceSync.PcfInstanceSyncPerpetualTaskParamsOuterClass.PcfInstanceSyncPerpetualTaskParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
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
public class PcfInstanceSyncPerpTaskClient implements PerpetualTaskServiceClient<PcfInstanceSyncPerpTaskClientParams> {
  static final boolean ALLOW_DUPLICATE_FALSE = false;
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject SecretManager secretManager;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;

  @Override
  public String create(String accountId, PcfInstanceSyncPerpTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(ID_KEY, clientParams.getInframappingId());
    clientParamMap.put(APPLICATION_ID, clientParams.getAppId());
    clientParamMap.put(APPLICATION_NAME, clientParams.getApplicationName());

    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();
    return perpetualTaskService.createTask(
        PerpetualTaskType.PCF_INSTANCE_SYNC, accountId, clientContext, schedule, ALLOW_DUPLICATE_FALSE);
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
                                                .timeoutIntervalInMin(TIMEOUT_SECONDS / 60)
                                                .build(),
                      encryptionDetails})
                  .timeout(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
                  .build())
        .build();
  }

  @VisibleForTesting
  PcfInstanceSyncPTDelegateParams getPcfInstanceSyncPTDelegateParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    return getPerpetualTaskParams(
        clientParams.get(ID_KEY), clientParams.get(APPLICATION_NAME), clientParams.get(APPLICATION_ID));
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    if (newPerpetualTaskResponse.getPerpetualTaskState().equals(PerpetualTaskState.TASK_RUN_FAILED)) {
      logger.info(
          "Resetting the perpetual task: {}, state: {}", taskId, newPerpetualTaskResponse.getPerpetualTaskState());
      PerpetualTaskRecord taskRecord = perpetualTaskService.getTaskRecord(taskId);
      perpetualTaskService.resetTask(taskRecord.getAccountId(), taskId);
    }
  }

  private PcfInstanceSyncPTDelegateParams getPerpetualTaskParamsInternal(
      PcfInfrastructureMapping pcfInfrastructureMapping, String applicationName, String appId) {
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
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof PcfInfrastructureMapping)) {
      String msg =
          "Incompatible infra mapping type. Expecting PCF type. Found:" + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw WingsException.builder().message(msg).build();
    }
    PcfInfrastructureMapping pcfInfrastructureMapping = (PcfInfrastructureMapping) infrastructureMapping;
    return getPerpetualTaskParamsInternal(pcfInfrastructureMapping, applicationName, appId);
  }
}
