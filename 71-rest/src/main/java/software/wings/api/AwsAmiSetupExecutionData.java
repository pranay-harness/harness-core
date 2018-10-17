package software.wings.api;

import com.google.common.collect.Maps;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.ResizeStrategy;
import software.wings.common.Constants;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by anubhaw on 12/20/17.
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsAmiSetupExecutionData extends StateExecutionData implements ResponseData {
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer newVersion;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;
  private ResizeStrategy resizeStrategy;
  private String activityId;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newHashMap();
    putNotNull(executionDetails, "newAutoScalingGroupName",
        ExecutionDataValue.builder().displayName("New ASG Name").value(newAutoScalingGroupName).build());
    putNotNull(executionDetails, "maxInstances",
        ExecutionDataValue.builder().displayName("Desired Capacity").value(maxInstances).build());
    putNotNull(executionDetails, "oldAutoScalingGroupName",
        ExecutionDataValue.builder().displayName("Old ASG Name").value(oldAutoScalingGroupName).build());
    putNotNull(executionDetails, Constants.ACTIVITY_ID,
        ExecutionDataValue.builder().displayName(Constants.ACTIVITY_ID).value(activityId).build());
    return executionDetails;
  }
}
