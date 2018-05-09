package software.wings.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KubernetesSteadyStateCheckExecutionData extends StateExecutionData implements NotifyResponseData {
  private String activityId;
  private String commandName;
  private Map<String, String> labels;
  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

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
    putNotNull(executionDetails, "labels",
        ExecutionDataValue.builder().value((labels != null) ? labels.toString() : null).displayName("Labels").build());

    return executionDetails;
  }

  @Override
  public KubernetesSteadyStateCheckExecutionSummary getStepExecutionSummary() {
    return KubernetesSteadyStateCheckExecutionSummary.builder().labels(labels).build();
  }
}
