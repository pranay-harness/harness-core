package software.wings.delegatetasks.azure.arm.deployment.context;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class DeploymentBlueprintSteadyStateContext {
  private AzureConfig azureConfig;
  private String assignmentResourceScope;
  private String assignmentName;
  private int steadyCheckTimeoutInMinutes;
  private long statusCheckIntervalInSeconds;
}
