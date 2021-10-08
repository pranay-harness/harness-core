package software.wings.delegatetasks.azure.arm.deployment.context;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.blueprint.Blueprint;
import io.harness.azure.model.blueprint.assignment.Assignment;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(HarnessTeam.CDP)
public class DeploymentBlueprintContext {
  private AzureConfig azureConfig;
  private String definitionResourceScope;
  private String blueprintName;
  private Blueprint existingBlueprint;
  private String blueprintJSON;
  private Map<String, String> artifacts;
  private String versionId;
  private Assignment assignment;
  private String assignmentJSON;
  private String roleAssignmentName;
  private String assignmentSubscriptionId;
  private String assignmentResourceScope;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private int steadyStateTimeoutInMin;
}
