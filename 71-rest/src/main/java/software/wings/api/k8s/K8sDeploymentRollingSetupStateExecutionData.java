package software.wings.api.k8s;

import com.google.common.collect.Maps;

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
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sDeploymentRollingSetupStateExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;
  private String accountId;
  private String appId;
  private String serviceId;
  private String envId;
  private String infraMappingId;
  private String commandName;

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
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());

    return executionDetails;
  }
}
