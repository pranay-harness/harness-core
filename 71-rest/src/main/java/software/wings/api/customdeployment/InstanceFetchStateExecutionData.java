package software.wings.api.customdeployment;

import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ExecutionDataValue;
import software.wings.api.InstanceFetchStateExecutionSummary;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class InstanceFetchStateExecutionData extends StateExecutionData implements Outcome {
  private String activityId;
  private String hostObjectArrayPath;
  private String instanceFetchScript;
  private Map<String, String> hostAttributes;
  private String scriptOutput;
  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionSummary = super.getExecutionSummary();
    setExecutionData(executionSummary);
    return executionSummary;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    setExecutionData(executionDetails);
    return executionDetails;
  }

  private void setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
  }

  @Override
  public InstanceFetchStateExecutionSummary getStepExecutionSummary() {
    return InstanceFetchStateExecutionSummary.builder()
        .activityId(activityId)
        .instanceFetchScript(instanceFetchScript)
        .scriptOutput(scriptOutput)
        .build();
  }
}
