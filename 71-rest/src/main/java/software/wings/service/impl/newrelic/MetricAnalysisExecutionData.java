package software.wings.service.impl.newrelic;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import java.util.Set;

/**
 * Created by anubhaw on 8/4/16.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricAnalysisExecutionData extends StateExecutionData {
  @JsonIgnore @Inject private MetricDataAnalysisService metricDataAnalysisService;

  private String appId;
  private String correlationId;
  private String workflowExecutionId;
  private String stateExecutionInstanceId;
  private String serverConfigId;
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
    final int total = timeDuration;
    putNotNull(executionDetails, "total", ExecutionDataValue.builder().displayName("Total").value(total).build());
    Set<String> crypticHostnames = Sets.newHashSet("testNode", "controlNode-1", "controlNode-2", "controlNode-3",
        "controlNode-4", "controlNode-5", "controlNode-6", "controlNode-7");
    Set<String> oldHostNames = lastExecutionNodes;
    Set<String> newHostNames = canaryNewHostNames;
    if (oldHostNames != null) {
      oldHostNames.removeAll(crypticHostnames);
    }
    if (newHostNames != null) {
      newHostNames.removeAll(crypticHostnames);
    }
    putNotNull(executionDetails, "timeDuration",
        ExecutionDataValue.builder().displayName("Analysis duration").value(timeDuration).build());
    putNotNull(executionDetails, "newVersionNodes",
        ExecutionDataValue.builder().displayName("New version nodes").value(newHostNames).build());
    putNotNull(executionDetails, "previousVersionNodes",
        ExecutionDataValue.builder().displayName("Previous version nodes").value(oldHostNames).build());
    return executionDetails;
  }
}
