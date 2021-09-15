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
public class DeploymentSubscriptionContext extends DeploymentContext {
  private AzureConfig azureConfig;
  private String subscriptionId;
  private String deploymentDataLocation;

  @Builder
  public DeploymentSubscriptionContext(@NotNull String deploymentName, @NotNull AzureConfig azureConfig,
      @NotNull String subscriptionId, @NotNull String deploymentDataLocation, @NotNull String templateJson,
      String parametersJson, AzureDeploymentMode mode, ILogStreamingTaskClient logStreamingTaskClient,
      int steadyStateTimeoutInMin) {
    super(deploymentName, mode, templateJson, parametersJson, logStreamingTaskClient, steadyStateTimeoutInMin, null);
    this.azureConfig = azureConfig;
    this.subscriptionId = subscriptionId;
    this.deploymentDataLocation = deploymentDataLocation;
  }
}
