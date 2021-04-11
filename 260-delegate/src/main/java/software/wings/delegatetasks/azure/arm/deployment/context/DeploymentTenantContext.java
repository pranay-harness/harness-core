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
public class DeploymentTenantContext extends DeploymentContext {
  private AzureConfig azureConfig;
  private String deploymentDataLocation;

  @Builder
  public DeploymentTenantContext(@NotNull String deploymentName, @NotNull AzureConfig azureConfig,
      @NotNull String deploymentDataLocation, @NotNull String templateJson, String parametersJson,
      AzureDeploymentMode mode, ILogStreamingTaskClient logStreamingTaskClient, int steadyStateTimeoutInMin) {
    super(deploymentName, mode, templateJson, parametersJson, logStreamingTaskClient, steadyStateTimeoutInMin, null);
    this.azureConfig = azureConfig;
    this.deploymentDataLocation = deploymentDataLocation;
  }
}
