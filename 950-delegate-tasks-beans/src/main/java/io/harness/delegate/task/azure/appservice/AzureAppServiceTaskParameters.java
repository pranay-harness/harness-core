package io.harness.delegate.task.azure.appservice;

import static com.google.common.collect.Sets.newHashSet;

import io.harness.delegate.task.azure.AzureTaskParameters;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureAppServiceTaskParameters extends AzureTaskParameters {
  private static final Set<AzureAppServiceTaskType> SYNC_TASK_TYPES =
      newHashSet(AzureAppServiceTaskParameters.AzureAppServiceTaskType.LIST_WEB_APP_NAMES,
          AzureAppServiceTaskType.LIST_WEB_APP_DEPLOYMENT_SLOT_NAMES);

  @NotNull private AzureAppServiceTaskType commandType;
  @NotNull private AzureAppServiceType appServiceType;
  @NotNull private String resourceGroupName;
  private String appName;

  public AzureAppServiceTaskParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String appName, String commandName, Integer timeoutIntervalInMin,
      AzureAppServiceTaskType commandType, AzureAppServiceType appServiceType) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin);
    this.commandType = commandType;
    this.appServiceType = appServiceType;
    this.resourceGroupName = resourceGroupName;
    this.appName = appName;
  }

  public enum AzureAppServiceTaskType {
    LIST_WEB_APP_NAMES,
    LIST_WEB_APP_DEPLOYMENT_SLOT_NAMES,
    SLOT_SETUP,
    SLOT_SHIFT_TRAFFIC,
    SLOT_SWAP,
    ROLLBACK
  }

  public enum AzureAppServiceType { WEB_APP, FUNCTION_APP, API_APP }

  public boolean isSyncTask() {
    return SYNC_TASK_TYPES.contains(commandType);
  }
}
