/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.azure.appservice.deployment.context;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureAppServiceDockerDeploymentContext extends AzureAppServiceDeploymentContext {
  private String imagePathAndTag;
  private Map<String, AzureAppServiceApplicationSetting> dockerSettings;

  @Builder
  public AzureAppServiceDockerDeploymentContext(AzureWebClientContext azureWebClientContext,
      ILogStreamingTaskClient logStreamingTaskClient, Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd,
      Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove,
      Map<String, AzureAppServiceConnectionString> connSettingsToAdd,
      Map<String, AzureAppServiceConnectionString> connSettingsToRemove,
      Map<String, AzureAppServiceApplicationSetting> dockerSettings, String imagePathAndTag, String slotName,
      String targetSlotName, int steadyStateTimeoutInMin) {
    super(azureWebClientContext, logStreamingTaskClient, appSettingsToAdd, appSettingsToRemove, connSettingsToAdd,
        connSettingsToRemove, slotName, targetSlotName, steadyStateTimeoutInMin);
    this.dockerSettings = dockerSettings;
    this.imagePathAndTag = imagePathAndTag;
  }
}
