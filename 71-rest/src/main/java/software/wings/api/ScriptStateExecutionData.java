package software.wings.api;

import io.harness.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class ScriptStateExecutionData extends StateExecutionData implements ResponseData {
  private String name;
  private String activityId;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    setExecutionData(executionDetails);
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    setExecutionData(executionDetails);
    return executionDetails;
  }

  @Override
  public ScriptStateExecutionSummary getStepExecutionSummary() {
    return ScriptStateExecutionSummary.builder().build();
  }

  private void setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
    putNotNull(executionDetails, "name", ExecutionDataValue.builder().displayName("Name").value(name).build());
  }
}
