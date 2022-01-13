/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.request;

import static com.google.common.collect.Sets.newHashSet;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
public class AzureVMSSTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private static final Set<AzureVMSSTaskParameters.AzureVMSSTaskType> SYNC_TASK_TYPES =
      newHashSet(AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_SUBSCRIPTIONS,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_RESOURCE_GROUPS_NAMES,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_VIRTUAL_MACHINE_SCALE_SETS,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_GET_VIRTUAL_MACHINE_SCALE_SET,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_VM_DATA,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_LOAD_BALANCERS_NAMES,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_LOAD_BALANCER_BACKEND_POOLS_NAMES);

  private String appId;
  private String accountId;
  private String activityId;
  private String commandName;
  private Integer timeoutIntervalInMin;
  @NotEmpty private AzureVMSSTaskParameters.AzureVMSSTaskType commandType;

  public enum AzureVMSSTaskType {
    AZURE_VMSS_LIST_SUBSCRIPTIONS,
    AZURE_VMSS_LIST_RESOURCE_GROUPS_NAMES,
    AZURE_VMSS_LIST_VIRTUAL_MACHINE_SCALE_SETS,
    AZURE_VMSS_LIST_LOAD_BALANCERS_NAMES,
    AZURE_VMSS_LIST_LOAD_BALANCER_BACKEND_POOLS_NAMES,
    AZURE_VMSS_GET_VIRTUAL_MACHINE_SCALE_SET,
    AZURE_VMSS_SETUP,
    AZURE_VMSS_DEPLOY,
    AZURE_VMSS_LIST_VM_DATA,
    AZURE_VMSS_SWITCH_ROUTE
  }

  public boolean isSyncTask() {
    return SYNC_TASK_TYPES.contains(commandType);
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.emptyList();
  }
}
