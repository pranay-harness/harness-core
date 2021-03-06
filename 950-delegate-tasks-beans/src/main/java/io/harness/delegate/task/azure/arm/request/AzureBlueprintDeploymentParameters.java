package io.harness.delegate.task.azure.arm.request;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;
import io.harness.expression.Expression;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureBlueprintDeploymentParameters extends AzureARMTaskParameters {
  @Expression(ALLOW_SECRETS) private String blueprintJson;
  @Expression(ALLOW_SECRETS) private Map<String, String> artifacts;
  @Expression(ALLOW_SECRETS) private String assignmentJson;

  @Builder
  public AzureBlueprintDeploymentParameters(String appId, String accountId, String activityId, String blueprintJson,
      Map<String, String> artifacts, String assignmentJson, String commandName, Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, null, commandName, timeoutIntervalInMin, AzureARMTaskType.BLUEPRINT_DEPLOYMENT);
    this.blueprintJson = blueprintJson;
    this.artifacts = artifacts;
    this.assignmentJson = assignmentJson;
  }
}
