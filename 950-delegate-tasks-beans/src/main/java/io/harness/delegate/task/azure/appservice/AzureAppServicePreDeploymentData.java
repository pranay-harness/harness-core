package io.harness.delegate.task.azure.appservice;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureAppServicePreDeploymentData {
  private String appName;
  private String slotName;
  private double trafficWeight;
  private Map<String, AzureAppServiceApplicationSetting> appSettings;
  private Map<String, AzureAppServiceConnectionString> connSettings;
  private Map<String, AzureAppServiceDockerSetting> dockerSettings;
}
