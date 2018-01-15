package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(callSuper = false)
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
  private boolean rollback;

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
        anExecutionDataValue().withValue(requestedCount).withDisplayName("Desired Instance Requested").build());
    if (resizeStrategy != null) {
      putNotNull(executionDetails, "resizeStrategy",
          anExecutionDataValue().withValue(resizeStrategy.getDisplayName()).withDisplayName("Resize Strategy").build());
    }
    putNotNull(executionDetails, "newAutoScalingGroupName",
        anExecutionDataValue().withValue(newAutoScalingGroupName).withDisplayName("New ASG").build());
    if (isNotEmpty(newInstanceData) && isNotBlank(newAutoScalingGroupName)) {
      int desiredCapacity =
          rollback ? newInstanceData.get(0).getPreviousCount() : newInstanceData.get(0).getDesiredCount();
      putNotNull(executionDetails, "newInstanceDataDesiredCapacity",
          anExecutionDataValue().withValue(desiredCapacity).withDisplayName("New ASG Desired Capacity").build());
    }
    putNotNull(executionDetails, "oldAutoScalingGroupName",
        anExecutionDataValue().withValue(oldAutoScalingGroupName).withDisplayName("Old ASG").build());
    if (isNotEmpty(oldInstanceData) && isNotBlank(oldAutoScalingGroupName)) {
      int desiredCapacity =
          rollback ? oldInstanceData.get(0).getPreviousCount() : oldInstanceData.get(0).getDesiredCount();
      putNotNull(executionDetails, "oldInstanceDataDesiredCapacity",
          anExecutionDataValue().withValue(desiredCapacity).withDisplayName("Old ASG Desired Capacity").build());
    }
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    return AmiStepExecutionSummary.builder()
        .instanceCount(instanceCount)
        .instanceUnitType(instanceUnitType)
        .newInstanceData(newInstanceData)
        .oldInstanceData(oldInstanceData)
        .build();
  }
}
