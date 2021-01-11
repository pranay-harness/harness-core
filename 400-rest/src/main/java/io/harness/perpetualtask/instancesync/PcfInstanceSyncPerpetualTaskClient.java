package io.harness.perpetualtask.instancesync;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.Cd1SetupFields;

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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PcfInstanceSyncPerpetualTaskClient implements PerpetualTaskServiceClient {
  public static final String PCF_APPLICATION_NAME = "pcfApplicationName";

  @Inject SecretManager secretManager;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    PcfInstanceSyncPTDelegateParams perpetualTaskParams = getPcfInstanceSyncPTDelegateParams(clientContext);

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(perpetualTaskParams.getPcfConfig(), null, null);

    ByteString configBytes = ByteString.copyFrom(kryoSerializer.asBytes(perpetualTaskParams.getPcfConfig()));
    ByteString encryptedConfigBytes = ByteString.copyFrom(kryoSerializer.asBytes(encryptionDetails));

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
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
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
                  .timeout(System.currentTimeMillis() + TaskData.DELEGATE_QUEUE_TIMEOUT)
                  .build())
        .build();
  }

  @VisibleForTesting
  PcfInstanceSyncPTDelegateParams getPcfInstanceSyncPTDelegateParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    return getPerpetualTaskParams(clientParams.get(INFRASTRUCTURE_MAPPING_ID), clientParams.get(PCF_APPLICATION_NAME),
        clientParams.get(HARNESS_APPLICATION_ID));
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
