package io.harness.delegate.task.azure.appservice.webapp.request;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppSlotSetupParameters extends AzureAppServiceTaskParameters {
  private String resourceGroupName;
  private String webAppName;
  private String slotName;
  private String imageName;
  private String imageTag;
  private Map<String, AzureAppServiceApplicationSetting> appSettings;
  private Map<String, AzureAppServiceConnectionString> connSettings;
  private Map<String, AzureAppServiceDockerSetting> dockerSettings;
  private long slotStoppingSteadyStateTimeoutInMinutes;
  private long slotStartingSteadyStateTimeoutInMinutes;

  @Builder
  public AzureWebAppSlotSetupParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String webAppName, String slotName, String imageName, String imageTag,
      Map<String, AzureAppServiceApplicationSetting> appSettings,
      Map<String, AzureAppServiceConnectionString> connSettings,
      Map<String, AzureAppServiceDockerSetting> dockerSettings, long slotStoppingSteadyStateTimeoutInMinutes,
      long slotStartingSteadyStateTimeoutInMinutes, String commandName, Integer timeoutIntervalInMin,
      AzureAppServiceTaskType commandType, AzureAppServiceType appServiceType) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin, commandType, appServiceType);
    this.resourceGroupName = resourceGroupName;
    this.webAppName = webAppName;
    this.slotName = slotName;
    this.imageName = imageName;
    this.imageTag = imageTag;
    this.appSettings = appSettings;
    this.connSettings = connSettings;
    this.dockerSettings = dockerSettings;
    this.slotStoppingSteadyStateTimeoutInMinutes = slotStoppingSteadyStateTimeoutInMinutes;
    this.slotStartingSteadyStateTimeoutInMinutes = slotStartingSteadyStateTimeoutInMinutes;
  }
}
