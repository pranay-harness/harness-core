package software.wings.api.ecs;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EcsRoute53WeightUpdateStateExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;
  private String newServiceName;
  private String newServiceDiscoveryServiceArn;
  private int newServiceWeight;
  private String oldServiceName;
  private String oldServiceDiscoveryServiceArn;
  private int oldServiceWeight;

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
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
    putNotNull(executionDetails, "New Service Name",
        ExecutionDataValue.builder().displayName("New Service Name").value(newServiceName).build());
    putNotNull(executionDetails, "New Service Weight",
        ExecutionDataValue.builder().displayName("New Service Weight").value(newServiceWeight).build());
    putNotNull(executionDetails, "New Service Discovery Service Arn",
        ExecutionDataValue.builder()
            .displayName("New Service Discovery Service Arn")
            .value(newServiceDiscoveryServiceArn)
            .build());
    putNotNull(executionDetails, "Old Service Name",
        ExecutionDataValue.builder().displayName("Old Service Name").value(oldServiceName).build());
    putNotNull(executionDetails, "Old Service Discovery Service Arn",
        ExecutionDataValue.builder()
            .displayName("Old Service Discovery Service Arn")
            .value(oldServiceDiscoveryServiceArn)
            .build());
    putNotNull(executionDetails, "Old Service Weight",
        ExecutionDataValue.builder().displayName("Old Service Weight").value(oldServiceWeight).build());
    return executionDetails;
  }
}