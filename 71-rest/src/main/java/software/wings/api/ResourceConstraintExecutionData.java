package software.wings.api;

import lombok.Getter;
import lombok.Setter;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;

public class ResourceConstraintExecutionData extends StateExecutionData {
  @Getter @Setter private String resourceConstraintName;
  @Getter @Setter private int resourceConstraintCapacity;
  @Getter @Setter private int usage;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "Name",
        ExecutionDataValue.builder().displayName("Name").value(resourceConstraintName).build());
    putNotNull(executionDetails, "Capacity",
        ExecutionDataValue.builder().displayName("Capacity").value(resourceConstraintCapacity).build());
    putNotNull(executionDetails, "Usage", ExecutionDataValue.builder().displayName("Usage").value(usage).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "Name",
        ExecutionDataValue.builder().displayName("Name").value(resourceConstraintName).build());
    putNotNull(executionDetails, "Capacity",
        ExecutionDataValue.builder().displayName("Capacity").value(resourceConstraintCapacity).build());
    putNotNull(executionDetails, "Usage", ExecutionDataValue.builder().displayName("Usage").value(usage).build());
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    ResourceConstraintStepExecutionSummary executionSummary = new ResourceConstraintStepExecutionSummary();
    populateStepExecutionSummary(executionSummary);
    executionSummary.setResourceConstraintName(resourceConstraintName);
    executionSummary.setResourceConstraintCapacity(resourceConstraintCapacity);
    executionSummary.setUsage(usage);
    return executionSummary;
  }
}
