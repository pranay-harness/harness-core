package io.harness.delegate.task.azure.appservice;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureAppServicePreDeploymentData {
  private String appName;
  private String slotName;
  private double trafficWeight;
  private Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove;
  private Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd;
  private Map<String, AzureAppServiceConnectionString> connSettingsToRemove;
  private Map<String, AzureAppServiceConnectionString> connSettingsToAdd;
  private String imageNameAndTag;
}
