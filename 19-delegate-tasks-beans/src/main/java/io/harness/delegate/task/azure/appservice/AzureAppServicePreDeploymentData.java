package io.harness.delegate.task.azure.appservice;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AzureAppServicePreDeploymentData {
  private String appName;
  private String slotName;
  private Map<String, AzureAppServiceApplicationSetting> appSettings;
  private Map<String, AzureAppServiceConnectionString> connSettings;
  private Map<String, AzureAppServiceDockerSetting> dockerSettings;
}
