package io.harness.delegate.task.azure.appservice;

import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceApplicationSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceConnectionStringDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceDockerSettingDTO;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureAppServicePreDeploymentData {
  private String appName;
  private String slotName;
  private double trafficWeight;
  private Map<String, AzureAppServiceApplicationSettingDTO> appSettingsToRemove;
  private Map<String, AzureAppServiceApplicationSettingDTO> appSettingsToAdd;
  private Map<String, AzureAppServiceConnectionStringDTO> connStringsToRemove;
  private Map<String, AzureAppServiceConnectionStringDTO> connStringsToAdd;
  private Map<String, AzureAppServiceDockerSettingDTO> dockerSettingsToAdd;
  private String imageNameAndTag;
  private AzureAppServiceTaskType failedTaskType;
}
