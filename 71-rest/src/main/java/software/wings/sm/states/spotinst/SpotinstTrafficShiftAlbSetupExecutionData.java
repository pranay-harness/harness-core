package software.wings.sm.states.spotinst;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.spotinst.model.ElastiGroup;
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
public class SpotinstTrafficShiftAlbSetupExecutionData
    extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String newElastigroupId;
  private String newElastigroupName;
  private String oldElastigroupId;
  private String oldElastigroupName;
  private String serviceId;
  private String envId;
  private String appId;
  private String infraMappingId;
  private String commandName;
  private ElastiGroup elastigroupOriginalConfig;

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
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
    putNotNull(executionDetails, "New Elastigroup Id",
        ExecutionDataValue.builder().value(newElastigroupId).displayName("New Elastigroup Id").build());
    putNotNull(executionDetails, "New Elastigroup Name",
        ExecutionDataValue.builder().value(newElastigroupName).displayName("New Elastigroup Name").build());
    putNotNull(executionDetails, "Old Elastigroup Id",
        ExecutionDataValue.builder().value(oldElastigroupId).displayName("Old Elastigroup Id").build());
    putNotNull(executionDetails, "Old Elastigroup Name",
        ExecutionDataValue.builder().value(oldElastigroupName).displayName("Old Elastigroup Name").build());
    return executionDetails;
  }

  @Override
  public SpotInstSetupExecutionSummary getStepExecutionSummary() {
    return SpotInstSetupExecutionSummary.builder().build();
  }
}