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
import software.wings.sm.states.spotinst.SpotinstDeployExecutionSummary.SpotinstDeployExecutionSummaryBuilder;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SpotinstTrafficShiftAlbSwapRoutesExecutionData
    extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String serviceId;
  private String envId;
  private String appId;
  private String infraMappingId;
  private String commandName;
  private ElastiGroup newElastigroupOriginalConfig;
  private ElastiGroup oldElastigroupOriginalConfig;
  private int newElastigroupWeight;

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
    if (oldElastigroupOriginalConfig != null) {
      putNotNull(executionDetails, "Old Elastigroup Id",
          ExecutionDataValue.builder()
              .value(oldElastigroupOriginalConfig.getId())
              .displayName("Old Elastigroup Id")
              .build());
      putNotNull(executionDetails, "Old Elastigroup Name",
          ExecutionDataValue.builder()
              .value(oldElastigroupOriginalConfig.getName())
              .displayName("Old Elastigroup Name")
              .build());
    }
    if (newElastigroupOriginalConfig != null) {
      putNotNull(executionDetails, "New Elastigroup Id",
          ExecutionDataValue.builder()
              .value(newElastigroupOriginalConfig.getId())
              .displayName("New Elastigroup Id")
              .build());
      putNotNull(executionDetails, "New Elastigroup Name",
          ExecutionDataValue.builder()
              .value(newElastigroupOriginalConfig.getName())
              .displayName("New Elastigroup Name")
              .build());
    }
    putNotNull(executionDetails, "New Elastigroup Weight",
        ExecutionDataValue.builder().value(newElastigroupWeight).displayName("New Elastigroup Weight").build());
    return executionDetails;
  }

  @Override
  public SpotinstDeployExecutionSummary getStepExecutionSummary() {
    SpotinstDeployExecutionSummaryBuilder builder = SpotinstDeployExecutionSummary.builder();
    if (oldElastigroupOriginalConfig != null) {
      builder.oldElastigroupId(oldElastigroupOriginalConfig.getId());
      builder.oldElastigroupName(oldElastigroupOriginalConfig.getName());
    }
    if (newElastigroupOriginalConfig != null) {
      builder.newElastigroupId(newElastigroupOriginalConfig.getId());
      builder.newElastigroupName(newElastigroupOriginalConfig.getName());
    }
    return builder.build();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }
}