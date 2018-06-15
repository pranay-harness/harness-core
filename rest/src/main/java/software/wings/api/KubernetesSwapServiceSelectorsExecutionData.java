package software.wings.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KubernetesSwapServiceSelectorsExecutionData extends StateExecutionData implements NotifyResponseData {
  private String activityId;
  private String commandName;
  private String service1;
  private String service2;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
    putNotNull(
        executionDetails, "service1", ExecutionDataValue.builder().value(service1).displayName("Service One").build());
    putNotNull(
        executionDetails, "service2", ExecutionDataValue.builder().value(service2).displayName("Service Two").build());

    return executionDetails;
  }
}
