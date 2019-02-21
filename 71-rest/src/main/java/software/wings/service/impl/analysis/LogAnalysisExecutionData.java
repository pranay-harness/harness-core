package software.wings.service.impl.analysis;

import static software.wings.common.VerificationConstants.DELAY_MINUTES;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import java.util.Set;

/**
 * Created by anubhaw on 8/4/16.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class LogAnalysisExecutionData extends StateExecutionData {
  private String correlationId;
  private String stateExecutionInstanceId;
  private String serverConfigId;
  private String query;
  private int timeDuration;
  private Set<String> canaryNewHostNames;
  private Set<String> lastExecutionNodes;
  private int analysisMinute;
  private String delegateTaskId;

  @Override
  @JsonIgnore
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = getExecutionDetails();
    putNotNull(executionDetails, "stateExecutionInstanceId",
        ExecutionDataValue.builder().displayName("State Execution Id").value(stateExecutionInstanceId).build());
    putNotNull(executionDetails, "serverConfigId",
        ExecutionDataValue.builder().displayName("Server Config Id").value(serverConfigId).build());
    return executionDetails;
  }

  @Override
  @JsonIgnore
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "errorMsg", ExecutionDataValue.builder().displayName("Message").value(getErrorMsg()).build());
    final int total = timeDuration + DELAY_MINUTES + 1;
    putNotNull(executionDetails, "total", ExecutionDataValue.builder().displayName("Total").value(total).build());
    putNotNull(executionDetails, "timeDuration",
        ExecutionDataValue.builder().displayName("Analysis duration").value(timeDuration).build());
    putNotNull(executionDetails, "queries", ExecutionDataValue.builder().displayName("Queries").value(query).build());
    putNotNull(executionDetails, "newVersionNodes",
        ExecutionDataValue.builder().displayName("New version nodes").value(canaryNewHostNames).build());
    putNotNull(executionDetails, "previousVersionNodes",
        ExecutionDataValue.builder().displayName("Previous version nodes").value(lastExecutionNodes).build());
    return executionDetails;
  }
}
