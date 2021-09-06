package software.wings.delegatetasks.azure.arm.deployment.context;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureClientContext;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.azure.arm.deployment.context.DeploymentContext;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class DeploymentResourceGroupContext extends DeploymentContext {
  private AzureClientContext azureClientContext;
  private boolean isRollback;

  @Builder
  public DeploymentResourceGroupContext(@NotNull String deploymentName, @NotNull AzureClientContext azureClientContext,
      @NotNull String templateJson, String parametersJson, AzureDeploymentMode mode,
      ILogStreamingTaskClient logStreamingTaskClient, int steadyStateTimeoutInMin, boolean isRollback) {
    super(deploymentName, mode, templateJson, parametersJson, logStreamingTaskClient, steadyStateTimeoutInMin, null);
    this.azureClientContext = azureClientContext;
    this.isRollback = isRollback;
  }
}
