package io.harness.delegate.task.azure.arm.request;

import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureARMRollbackParameters extends AzureARMTaskParameters {
  @Builder
  public AzureARMRollbackParameters(String appId, String accountId, String activityId, String subscriptionId,
      String commandName, Integer timeoutIntervalInMin) {
    super(
        appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin, AzureARMTaskType.ARM_ROLLBACK);
  }
}
