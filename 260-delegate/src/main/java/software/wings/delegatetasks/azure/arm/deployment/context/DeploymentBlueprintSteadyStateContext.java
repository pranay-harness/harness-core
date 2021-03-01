package software.wings.delegatetasks.azure.arm.deployment.context;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(Module._930_DELEGATE_TASKS)
public class DeploymentBlueprintSteadyStateContext {
  private AzureConfig azureConfig;
  private String resourceScope;
  private String assignmentName;
  private int steadyCheckTimeoutInMinutes;
  private long statusCheckIntervalInSeconds;
}
