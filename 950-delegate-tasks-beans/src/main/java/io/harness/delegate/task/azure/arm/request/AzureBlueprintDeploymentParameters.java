package io.harness.delegate.task.azure.arm.request;

import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureBlueprintDeploymentParameters extends AzureARMTaskParameters {
  private String blueprintJson;
  private Map<String, String> artifacts;
  private String assignmentJson;

  @Builder
  public AzureBlueprintDeploymentParameters(String appId, String accountId, String activityId, String blueprintJson,
      Map<String, String> artifacts, String assignmentJson, String commandName, Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, null, commandName, timeoutIntervalInMin, AzureARMTaskType.BLUEPRINT_DEPLOYMENT);
    this.blueprintJson = blueprintJson;
    this.artifacts = artifacts;
    this.assignmentJson = assignmentJson;
  }
}
