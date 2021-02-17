package software.wings.delegatetasks.azure.arm.deployment.context;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(Module._930_DELEGATE_TASKS)
public class DeploymentContext {
  private String deploymentName;
  private AzureDeploymentMode mode;
  private String templateJson;
  private String parametersJson;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private int steadyStateTimeoutInMin;
}
