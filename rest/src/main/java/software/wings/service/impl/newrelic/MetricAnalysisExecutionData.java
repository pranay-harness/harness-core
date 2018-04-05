package software.wings.service.impl.newrelic;

import com.google.inject.Inject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.CountsByStatuses;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;

import java.util.Map;
import java.util.Set;

/**
 * Created by anubhaw on 8/4/16.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class MetricAnalysisExecutionData extends StateExecutionData {
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  private String correlationId;
  private String workflowExecutionId;
  private String stateExecutionInstanceId;
  private String serverConfigId;
  private int timeDuration;
  private Set<String> canaryNewHostNames;
  private Set<String> lastExecutionNodes;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = getExecutionDetails();
    putNotNull(executionDetails, "stateExecutionInstanceId",
        ExecutionDataValue.builder().displayName("State Execution Id").value(stateExecutionInstanceId).build());
    putNotNull(executionDetails, "serverConfigId",
        ExecutionDataValue.builder().displayName("Server Config Id").value(serverConfigId).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "errorMsg", ExecutionDataValue.builder().displayName("Message").value(getErrorMsg()).build());
    final int total = timeDuration;
    putNotNull(executionDetails, "total", ExecutionDataValue.builder().displayName("Total").value(total).build());
    final NewRelicMetricAnalysisRecord analysisRecord = metricDataAnalysisService.getMetricsAnalysis(
        StateType.valueOf(getStateType()), stateExecutionInstanceId, workflowExecutionId);

    int elapsedMinutes = analysisRecord == null ? 0 : analysisRecord.getAnalysisMinute();
    final CountsByStatuses breakdown = new CountsByStatuses();
    switch (getStatus()) {
      case FAILED:
        breakdown.setFailed(total);
        break;
      case SUCCESS:
        breakdown.setSuccess(total);
        break;
      default:
        breakdown.setSuccess(Math.min(elapsedMinutes, total));
        break;
    }
    putNotNull(
        executionDetails, "breakdown", ExecutionDataValue.builder().displayName("breakdown").value(breakdown).build());
    putNotNull(executionDetails, "timeDuration",
        ExecutionDataValue.builder().displayName("Analysis duration").value(timeDuration).build());
    return executionDetails;
  }
}
