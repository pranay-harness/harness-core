package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.mixin.IgnoreValidationCapabilityGenerator.buildIgnoreValidationCapability;
import static java.util.Collections.singletonList;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@AllArgsConstructor
public class SpotInstTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String appId;
  private String accountId;
  private String activityId;
  private String commandName;
  private String workflowExecutionId;
  private Integer timeoutIntervalInMin;
  @NotEmpty private SpotInstTaskType commandType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return singletonList(buildIgnoreValidationCapability());
  }

  public enum SpotInstTaskType { SPOT_INST_SETUP, SPOT_INST_DEPLOY }
}
