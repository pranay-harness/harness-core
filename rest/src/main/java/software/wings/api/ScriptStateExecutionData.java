package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

@Builder
@Data
public class ScriptStateExecutionData extends StateExecutionData implements NotifyResponseData {
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

  private void setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(executionDetails, "activityId",
        anExecutionDataValue().withValue(activityId).withDisplayName("Activity Id").build());
    putNotNull(executionDetails, "name", anExecutionDataValue().withValue(name).withDisplayName("Name").build());
  }
}
