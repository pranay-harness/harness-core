package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_SETUP;
import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType.WEB_APP;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppSlotSetupParameters extends AzureAppServiceTaskParameters {
  private String slotName;
  private Map<String, AzureAppServiceApplicationSetting> appSettings;
  private Map<String, AzureAppServiceConnectionString> connSettings;
  private String imageName;
  private String imageTag;
  ConnectorConfigDTO connectorConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  AzureRegistryType azureRegistryType;

  @Builder
  public AzureWebAppSlotSetupParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String webAppName, String slotName, String imageName, String imageTag,
      Map<String, AzureAppServiceApplicationSetting> appSettings,
      Map<String, AzureAppServiceConnectionString> connSettings, String commandName, Integer timeoutIntervalInMin,
      ConnectorConfigDTO connectorConfigDTO, List<EncryptedDataDetail> encryptedDataDetails,
      AzureRegistryType azureRegistryType) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, webAppName, commandName,
        timeoutIntervalInMin, SLOT_SETUP, WEB_APP);
    this.slotName = slotName;
    this.imageName = imageName;
    this.imageTag = imageTag;
    this.appSettings = appSettings;
    this.connSettings = connSettings;
    this.connectorConfigDTO = connectorConfigDTO;
    this.encryptedDataDetails = encryptedDataDetails;
    this.azureRegistryType = azureRegistryType;
  }
}