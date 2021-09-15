package io.harness.delegate.azure.arm.deployment.context;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
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
