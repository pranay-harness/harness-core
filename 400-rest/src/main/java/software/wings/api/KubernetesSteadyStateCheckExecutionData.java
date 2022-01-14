package software.wings.api;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.beans.container.Label;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class KubernetesSteadyStateCheckExecutionData
    extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String commandName;
  private List<Label> labels;
  private String namespace;
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
    putNotNull(
        executionDetails, "namespace", ExecutionDataValue.builder().value(namespace).displayName("Namespace").build());

    return executionDetails;
  }

  @Override
  public KubernetesSteadyStateCheckExecutionSummary getStepExecutionSummary() {
    return KubernetesSteadyStateCheckExecutionSummary.builder().labels(labels).namespace(namespace).build();
  }
}
