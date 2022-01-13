/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.arm.deployment.context;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class DeploymentManagementGroupContext extends DeploymentContext {
  private AzureConfig azureConfig;
  private String deploymentDataLocation;
  private String managementGroupId;

  @Builder
  public DeploymentManagementGroupContext(@NotNull String deploymentName, @NotNull AzureConfig azureConfig,
      @NotNull String managementGroupId, @NotNull String deploymentDataLocation, @NotNull String templateJson,
      String parametersJson, AzureDeploymentMode mode, ILogStreamingTaskClient logStreamingTaskClient,
      int steadyStateTimeoutInMin) {
    super(deploymentName, mode, templateJson, parametersJson, logStreamingTaskClient, steadyStateTimeoutInMin, null);
    this.azureConfig = azureConfig;
    this.managementGroupId = managementGroupId;
    this.deploymentDataLocation = deploymentDataLocation;
  }
}
