package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ResizeStrategy;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
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
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
  private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

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
    String requestedCount = instanceCount + " " + (instanceUnitType == InstanceUnitType.PERCENTAGE ? "%" : "");
    putNotNull(executionDetails, "requestedCount",
        anExecutionDataValue().withValue(requestedCount).withDisplayName("Requested Instances").build());
    putNotNull(executionDetails, "resizeStrategy",
        anExecutionDataValue().withValue(resizeStrategy).withDisplayName("Resize Strategy").build());
    putNotNull(executionDetails, "newAutoScalingGroupName",
        anExecutionDataValue().withValue(newAutoScalingGroupName).withDisplayName("New AutoScalingGroup").build());
    if (newInstanceData != null && !newInstanceData.isEmpty()) {
      putNotNull(executionDetails, "newInstanceDataDesiredCapacity",
          anExecutionDataValue()
              .withValue(newInstanceData.get(0).getDesiredCount())
              .withDisplayName("New ASG Desired Capacity")
              .build());
    }
    putNotNull(executionDetails, "oldAutoScalingGroupName",
        anExecutionDataValue().withValue(oldAutoScalingGroupName).withDisplayName("Old AutoScalingGroup").build());
    if (oldInstanceData != null && !oldInstanceData.isEmpty()) {
      putNotNull(executionDetails, "oldInstanceDataDesiredCapacity",
          anExecutionDataValue()
              .withValue(oldInstanceData.get(0).getDesiredCount())
              .withDisplayName("Old ASG Desired Capacity")
              .build());
    }
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    return AmiStepExecutionSummary.builder().newInstanceData(newInstanceData).oldInstanceData(oldInstanceData).build();
  }
}
