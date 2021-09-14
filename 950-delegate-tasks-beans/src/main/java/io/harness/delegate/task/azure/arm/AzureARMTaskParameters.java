/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.arm;

import static com.google.common.collect.Sets.newHashSet;

import io.harness.delegate.task.azure.AzureTaskParameters;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureARMTaskParameters extends AzureTaskParameters {
  private static final Set<AzureARMTaskParameters.AzureARMTaskType> SYNC_TASK_TYPES =
      newHashSet(AzureARMTaskType.LIST_SUBSCRIPTION_LOCATIONS, AzureARMTaskType.LIST_MNG_GROUP);
  @NotNull private AzureARMTaskParameters.AzureARMTaskType commandType;

  public AzureARMTaskParameters(String appId, String accountId, String activityId, String subscriptionId,
      String commandName, Integer timeoutIntervalInMin, AzureARMTaskParameters.AzureARMTaskType commandType) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin);
    this.commandType = commandType;
  }

  public enum AzureARMTaskType {
    ARM_DEPLOYMENT,
    ARM_ROLLBACK,
    LIST_SUBSCRIPTION_LOCATIONS,
    LIST_MNG_GROUP,
    BLUEPRINT_DEPLOYMENT
  }

  public boolean isSyncTask() {
    return SYNC_TASK_TYPES.contains(commandType);
  }
}
