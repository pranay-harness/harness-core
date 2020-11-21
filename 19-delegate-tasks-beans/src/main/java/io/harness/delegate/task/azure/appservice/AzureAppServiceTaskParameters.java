package io.harness.delegate.task.azure.appservice;

import static com.google.common.collect.Sets.newHashSet;

import io.harness.delegate.task.azure.AzureTaskParameters;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureAppServiceTaskParameters extends AzureTaskParameters {
  private static final Set<AzureAppServiceTaskType> SYNC_TASK_TYPES =
      newHashSet(AzureAppServiceTaskParameters.AzureAppServiceTaskType.LIST_WEB_APP_NAMES,
          AzureAppServiceTaskType.LIST_WEB_APP_DEPLOYMENT_SLOT_NAMES);

  @NotEmpty private AzureAppServiceTaskType commandType;
  @NotEmpty private AzureAppServiceType appServiceType;

  public AzureAppServiceTaskParameters(String appId, String accountId, String activityId, String subscriptionId,
      String commandName, Integer timeoutIntervalInMin, AzureAppServiceTaskType commandType,
      AzureAppServiceType appServiceType) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin);
    this.commandType = commandType;
    this.appServiceType = appServiceType;
  }

  public enum AzureAppServiceTaskType { LIST_WEB_APP_NAMES, LIST_WEB_APP_DEPLOYMENT_SLOT_NAMES, SLOT_SETUP }

  public enum AzureAppServiceType { WEB_APP, FUNCTION_APP, API_APP }

  public boolean isSyncTask() {
    return SYNC_TASK_TYPES.contains(commandType);
  }
}
