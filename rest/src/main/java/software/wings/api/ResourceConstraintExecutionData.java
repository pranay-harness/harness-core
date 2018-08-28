package software.wings.api;

import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

public class ResourceConstraintExecutionData extends StateExecutionData {
  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    ResourceConstraintStepExecutionSummary executionSummary = new ResourceConstraintStepExecutionSummary();
    populateStepExecutionSummary(executionSummary);
    return executionSummary;
  }
}
