package software.wings.delegatetasks.azure.arm.deployment.context;

import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentContext {
  private String deploymentName;
  private AzureDeploymentMode mode;
  private String templateJson;
  private String parametersJson;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private int steadyStateTimeoutInMin;
}
