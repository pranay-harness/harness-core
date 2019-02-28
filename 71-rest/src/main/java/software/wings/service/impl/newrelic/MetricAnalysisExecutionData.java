package software.wings.service.impl.newrelic;

import static io.harness.beans.ExecutionStatus.ERROR;

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
import software.wings.beans.CountsByStatuses;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
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
  @JsonIgnore @Inject private WingsPersistence wingsPersistence;

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

    int elapsedMinutes = (int) Math.max(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
                                            .filter("appId", appId)
                                            .filter("stateExecutionId", stateExecutionInstanceId)
                                            .count(),
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("appId", appId)
            .filter("stateExecutionId", stateExecutionInstanceId)
            .count());
    final CountsByStatuses breakdown = new CountsByStatuses();
    switch (getStatus()) {
      case ERROR:
        break;
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

    if (!ERROR.equals(getStatus())) {
      putNotNull(executionDetails, "breakdown",
          ExecutionDataValue.builder().displayName("breakdown").value(breakdown).build());
    }
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
