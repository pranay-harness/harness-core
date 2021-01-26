package software.wings.delegatetasks.azure.arm.deployment.context;

import io.harness.azure.context.AzureClientContext;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeploymentResourceGroupContext extends DeploymentContext {
  private AzureClientContext azureClientContext;

  @Builder
  public DeploymentResourceGroupContext(@NotNull String deploymentName, @NotNull AzureClientContext azureClientContext,
      @NotNull String templateJson, String parametersJson, AzureDeploymentMode mode,
      ILogStreamingTaskClient logStreamingTaskClient, int steadyStateTimeoutInMin) {
    super(deploymentName, mode, templateJson, parametersJson, logStreamingTaskClient, steadyStateTimeoutInMin);
    this.azureClientContext = azureClientContext;
  }
}
