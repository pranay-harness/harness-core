/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
