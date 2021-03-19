package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.beans.ResizeStrategy;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 12/20/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class AwsAmiSetupExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer newVersion;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;
  private Integer desiredInstances;
  private ResizeStrategy resizeStrategy;
  private String activityId;
  private static final String ACTIVITY_ID = "activityId";

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "newAutoScalingGroupName",
        ExecutionDataValue.builder().displayName("New ASG Name").value(newAutoScalingGroupName).build());
    putNotNull(executionDetails, "maxInstances",
        ExecutionDataValue.builder().displayName("Max Instances").value(maxInstances).build());
    putNotNull(executionDetails, "desiredInstances",
        ExecutionDataValue.builder().displayName("Desired Instances").value(desiredInstances).build());

    putNotNull(executionDetails, "oldAutoScalingGroupName",
        ExecutionDataValue.builder().displayName("Old ASG Name").value(oldAutoScalingGroupName).build());
    putNotNull(
        executionDetails, ACTIVITY_ID, ExecutionDataValue.builder().displayName(ACTIVITY_ID).value(activityId).build());
    return executionDetails;
  }
}
