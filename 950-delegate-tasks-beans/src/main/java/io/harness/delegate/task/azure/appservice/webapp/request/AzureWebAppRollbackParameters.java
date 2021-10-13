package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_ROLLBACK;
import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType.WEB_APP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.expression.Expression;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppRollbackParameters extends AzureAppServiceTaskParameters {
  private AzureAppServicePreDeploymentData preDeploymentData;
  @Expression(ALLOW_SECRETS) private String startupCommand;

  @Builder
  public AzureWebAppRollbackParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String appName, AzureAppServicePreDeploymentData preDeploymentData, String commandName,
      Integer timeoutIntervalInMin, String startupCommand) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, appName, commandName, timeoutIntervalInMin,
        SLOT_ROLLBACK, WEB_APP);
    this.preDeploymentData = preDeploymentData;
    this.startupCommand = startupCommand;
  }
}
