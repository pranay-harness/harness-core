package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.ResizeStrategy;
import software.wings.common.Constants;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by anubhaw on 12/20/17.
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsAmiSetupExecutionData extends StateExecutionData implements NotifyResponseData {
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
        anExecutionDataValue().withValue(newAutoScalingGroupName).withDisplayName("New ASG Name").build());
    putNotNull(executionDetails, "maxInstances",
        anExecutionDataValue().withValue(maxInstances).withDisplayName("Desired Capacity").build());
    putNotNull(executionDetails, "oldAutoScalingGroupName",
        anExecutionDataValue().withValue(oldAutoScalingGroupName).withDisplayName("Old ASG Name").build());
    putNotNull(executionDetails, Constants.SHOW_LOGS,
        anExecutionDataValue().withValue(true).withDisplayName(Constants.SHOW_LOGS).build());
    return executionDetails;
  }
}
