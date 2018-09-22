package software.wings.api.pcf;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcfSetupStateExecutionData extends StateExecutionData implements NotifyResponseData {
  private String activityId;
  private String accountId;
  private String appId;
  private String serviceId;
  private String envId;
  private String infraMappingId;
  private PcfCommandRequest pcfCommandRequest;
  private String commandName;
  private Integer maxInstanceCount;
  private List<String> routeMaps;
  private List<String> tempRouteMaps;
  private boolean rollback;
  private boolean isStandardBlueGreen;
  private boolean useTempRoutes;

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
    putNotNull(executionDetails, "organization",
        ExecutionDataValue.builder().value(pcfCommandRequest.getOrganization()).displayName("Organization").build());
    putNotNull(executionDetails, "space",
        ExecutionDataValue.builder().value(pcfCommandRequest.getSpace()).displayName("Space").build());
    putNotNull(executionDetails, "routeMaps",
        ExecutionDataValue.builder().value(String.valueOf(routeMaps)).displayName("Route Maps").build());
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());

    return executionDetails;
  }

  @Override
  public PcfSetupExecutionSummary getStepExecutionSummary() {
    return PcfSetupExecutionSummary.builder()
        .maxInstanceCount(maxInstanceCount)
        .organization(pcfCommandRequest.getOrganization())
        .space(pcfCommandRequest.getSpace())
        .build();
  }
}
