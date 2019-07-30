package software.wings.sm.states.spotinst;

import com.google.common.collect.Maps;

import io.harness.delegate.beans.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.api.pcf.PcfSetupExecutionSummary;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SpotInstSetupStateExecutionData extends StateExecutionData implements ResponseData {
  private String serviceId;
  private String envId;
  private String infraMappingId;
  private String commandName;
  private Integer maxInstanceCount;
  private boolean useCurrentRunningInstanceCount;
  private Integer currentRunningInstanceCount;
  private boolean rollback;
  private SpotInstCommandRequest spotinstCommandRequest;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newLinkedHashMap();
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder()
            .value(spotinstCommandRequest.getSpotInstTaskParameters().getActivityId())
            .displayName("Activity Id")
            .build());

    return executionDetails;
  }

  @Override
  public PcfSetupExecutionSummary getStepExecutionSummary() {
    return PcfSetupExecutionSummary.builder()
        .maxInstanceCount(useCurrentRunningInstanceCount ? currentRunningInstanceCount : maxInstanceCount)
        .build();
  }
}
