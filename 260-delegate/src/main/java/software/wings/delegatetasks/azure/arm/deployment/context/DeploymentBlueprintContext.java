package software.wings.delegatetasks.azure.arm.deployment.context;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(Module._930_DELEGATE_TASKS)
public class DeploymentBlueprintContext {
  private AzureConfig azureConfig;
  private String resourceScope;
  private String blueprintId;
  private String blueprintName;
  private String blueprintJSON;
  private Map<String, String> artifacts;
  private String versionId;
  private String assignmentName;
  private String assignmentJSON;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private int steadyStateTimeoutInMin;
}
