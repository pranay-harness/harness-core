package software.wings.api.customdeployment;

import io.harness.pms.sdk.core.data.Outcome;

import software.wings.api.ExecutionDataValue;
import software.wings.api.InstanceFetchStateExecutionSummary;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("instanceFetchStateExecutionData")
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

  @Override
  public String getType() {
    return "instanceFetchStateExecutionData";
  }
}
