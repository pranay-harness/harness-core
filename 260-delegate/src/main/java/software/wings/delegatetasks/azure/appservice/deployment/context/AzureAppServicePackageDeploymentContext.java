/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.deployment.context;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import software.wings.beans.artifact.ArtifactStreamAttributes;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureAppServicePackageDeploymentContext extends AzureAppServiceDeploymentContext {
  private ArtifactStreamAttributes artifactStreamAttributes;
  private String startupCommand;

  @Builder
  public AzureAppServicePackageDeploymentContext(AzureWebClientContext azureWebClientContext,
      ILogStreamingTaskClient logStreamingTaskClient, Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd,
      Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove,
      Map<String, AzureAppServiceConnectionString> connSettingsToAdd,
      Map<String, AzureAppServiceConnectionString> connSettingsToRemove,
      ArtifactStreamAttributes artifactStreamAttributes, String startupCommand, String slotName, String targetSlotName,
      int steadyStateTimeoutInMin) {
    super(azureWebClientContext, logStreamingTaskClient, appSettingsToAdd, appSettingsToRemove, connSettingsToAdd,
        connSettingsToRemove, slotName, targetSlotName, steadyStateTimeoutInMin);
    this.artifactStreamAttributes = artifactStreamAttributes;
    this.startupCommand = startupCommand;
  }
}
