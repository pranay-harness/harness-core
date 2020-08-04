package io.harness.delegate.task.azure.request;

import static com.google.common.collect.Sets.newHashSet;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class AzureVMSSTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private static final Set<AzureVMSSTaskParameters.AzureVMSSTaskType> SYNC_TASK_TYPES =
      newHashSet(AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_SUBSCRIPTIONS,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_RESOURCE_GROUPS_NAMES,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_VIRTUAL_MACHINE_SCALE_SETS,
          AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_GET_VIRTUAL_MACHINE_SCALE_SET);

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
    AZURE_VMSS_GET_VIRTUAL_MACHINE_SCALE_SET,
    AZURE_VMSS_SETUP,
  }

  public boolean isSyncTask() {
    return SYNC_TASK_TYPES.contains(commandType);
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.emptyList();
  }
}
