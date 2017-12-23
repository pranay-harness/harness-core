package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ResizeStrategy;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by anubhaw on 12/22/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsAmiDeployStateExecutionData extends StateExecutionData implements NotifyResponseData {
  private String activityId;
  private String commandName;
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer newVersion;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;
  private ResizeStrategy resizeStrategy;
  private int instanceCount;
  private InstanceUnitType instanceUnitType;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newLinkedHashMap();
    putNotNull(executionDetails, "activityId",
        anExecutionDataValue().withValue(activityId).withDisplayName("Activity Id").build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withValue(commandName).withDisplayName("Command Name").build());
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    return AmiStepExecutionSummary.builder()
        .activityId(activityId)
        .commandName(commandName)
        .autoScalingSteadyStateTimeout(autoScalingSteadyStateTimeout)
        .maxInstances(maxInstances)
        .newVersion(newVersion)
        .newAutoScalingGroupName(newAutoScalingGroupName)
        .oldAutoScalingGroupName(oldAutoScalingGroupName)
        .resizeStrategy(resizeStrategy)
        .instanceUnitType(instanceUnitType)
        .build();
  }
}
