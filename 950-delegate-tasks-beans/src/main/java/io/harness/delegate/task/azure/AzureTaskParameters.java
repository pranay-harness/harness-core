package io.harness.delegate.task.azure;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.Cd1ApplicationAccess;
import io.harness.delegate.task.TaskParameters;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AzureTaskParameters
    implements TaskParameters, ExecutionCapabilityDemander, ActivityAccess, Cd1ApplicationAccess {
  private String appId;
  private String accountId;
  private String activityId;
  private String subscriptionId;
  private String commandName;
  private Integer timeoutIntervalInMin;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.emptyList();
  }
}
