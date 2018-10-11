package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Maps;

import io.harness.task.protocol.ResponseData;
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
public class AwsAmiDeployStateExecutionData extends StateExecutionData implements ResponseData {
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
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
    String requestedCount = instanceCount + " " + (instanceUnitType == InstanceUnitType.PERCENTAGE ? "%" : "");
    putNotNull(executionDetails, "requestedCount",
        ExecutionDataValue.builder().displayName("Desired Instance Requested").value(requestedCount).build());
    if (resizeStrategy != null) {
      putNotNull(executionDetails, "resizeStrategy",
          ExecutionDataValue.builder().displayName("Resize Strategy").value(resizeStrategy.getDisplayName()).build());
    }
    putNotNull(executionDetails, "newAutoScalingGroupName",
        ExecutionDataValue.builder().displayName("New ASG").value(newAutoScalingGroupName).build());
    if (isNotEmpty(newInstanceData) && isNotBlank(newAutoScalingGroupName)) {
      int desiredCapacity =
          rollback ? newInstanceData.get(0).getPreviousCount() : newInstanceData.get(0).getDesiredCount();
      putNotNull(executionDetails, "newInstanceDataDesiredCapacity",
          ExecutionDataValue.builder().displayName("New ASG Desired Capacity").value(desiredCapacity).build());
    }
    putNotNull(executionDetails, "oldAutoScalingGroupName",
        ExecutionDataValue.builder().displayName("Old ASG").value(oldAutoScalingGroupName).build());
    if (isNotEmpty(oldInstanceData) && isNotBlank(oldAutoScalingGroupName)) {
      int desiredCapacity =
          rollback ? oldInstanceData.get(0).getPreviousCount() : oldInstanceData.get(0).getDesiredCount();
      putNotNull(executionDetails, "oldInstanceDataDesiredCapacity",
          ExecutionDataValue.builder().displayName("Old ASG Desired Capacity").value(desiredCapacity).build());
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
