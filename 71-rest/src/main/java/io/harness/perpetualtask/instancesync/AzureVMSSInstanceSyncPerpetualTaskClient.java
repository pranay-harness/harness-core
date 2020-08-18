package io.harness.perpetualtask.instancesync;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.azure.request.AzureVMSSListVMDataParameters;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.Cd1SetupFields;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.impl.azure.manager.AzureVMSSCommandRequest;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureVMSSInstanceSyncPerpetualTaskClient implements PerpetualTaskServiceClient {
  private static final String VMSS_ID = "vmssId";
  @Inject InfrastructureMappingService infraMappingService;
  @Inject SettingsService settingsService;
  @Inject SecretManager secretManager;
  @Inject KryoSerializer kryoSerializer;

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    PerpetualTaskData perpetualTaskData = getPerpetualTaskData(clientContext);

    ByteString azureConfigBytes = ByteString.copyFrom(kryoSerializer.asBytes(perpetualTaskData.getAzureConfig()));
    ByteString encryptedDataBytes =
        ByteString.copyFrom(kryoSerializer.asBytes(perpetualTaskData.getEncryptedDataDetails()));

    return AzureVmssInstanceSyncPerpetualTaskParams.newBuilder()
        .setSubscriptionId(perpetualTaskData.getSubscriptionId())
        .setResourceGroupName(perpetualTaskData.getResourceGroupName())
        .setVmssId(perpetualTaskData.getVmssId())
        .setAzureConfig(azureConfigBytes)
        .setAzureEncryptedData(encryptedDataBytes)
        .build();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    PerpetualTaskData perpetualTaskData = getPerpetualTaskData(clientContext);
    AzureVMSSListVMDataParameters azureVMSSListVMDataParameters =
        AzureVMSSListVMDataParameters.builder()
            .subscriptionId(perpetualTaskData.getSubscriptionId())
            .resourceGroupName(perpetualTaskData.getResourceGroupName())
            .vmssId(perpetualTaskData.getVmssId())
            .build();
    AzureVMSSCommandRequest request = AzureVMSSCommandRequest.builder()
                                          .azureConfig(perpetualTaskData.getAzureConfig())
                                          .azureEncryptionDetails(perpetualTaskData.getEncryptedDataDetails())
                                          .azureVMSSTaskParameters(azureVMSSListVMDataParameters)
                                          .build();

    return DelegateTask.builder()
        .accountId(accountId)
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.AZURE_VMSS_COMMAND_TASK.name())
                  .parameters(new Object[] {request})
                  .build())
        .build();
  }

  private PerpetualTaskData getPerpetualTaskData(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String appId = clientParams.get(HARNESS_APPLICATION_ID);
    String infraMappingId = clientParams.get(INFRASTRUCTURE_MAPPING_ID);
    String vmssId = clientParams.get(VMSS_ID);
    AzureVMSSInfrastructureMapping infraMapping =
        (AzureVMSSInfrastructureMapping) infraMappingService.get(appId, infraMappingId);

    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    AzureConfig azureConfig = (AzureConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(azureConfig, null, null);

    return PerpetualTaskData.builder()
        .subscriptionId(infraMapping.getSubscriptionId())
        .resourceGroupName(infraMapping.getResourceGroupName())
        .vmssId(vmssId)
        .azureConfig(azureConfig)
        .encryptedDataDetails(encryptedDataDetails)
        .build();
  }

  @Data
  @Builder
  static class PerpetualTaskData {
    private String subscriptionId;
    private String resourceGroupName;
    private String vmssId;
    private AzureConfig azureConfig;
    private List<EncryptedDataDetail> encryptedDataDetails;
  }
}