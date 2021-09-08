package io.harness.delegate.azure.arm.deployment.context;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class DeploymentContext {
  private String deploymentName;
  private AzureDeploymentMode mode;
  private String templateJson;
  private String parametersJson;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private int steadyStateTimeoutInMin;
  private String runningCommandUnit;
}
