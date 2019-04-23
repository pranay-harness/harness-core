package software.wings.verification;

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
import lombok.experimental.FieldNameConstants;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.CountsByStatuses;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@FieldNameConstants(innerTypeName = "VerificationStateAnalysisExecutionDataKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerificationStateAnalysisExecutionData extends StateExecutionData {
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
  private MLAnalysisType mlAnalysisType;
  private String query;

  @Override
  @JsonIgnore
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = getExecutionDetails();
    putNotNull(executionDetails, VerificationStateAnalysisExecutionDataKeys.stateExecutionInstanceId,
        ExecutionDataValue.builder().displayName("State Execution Id").value(stateExecutionInstanceId).build());
    putNotNull(executionDetails, VerificationStateAnalysisExecutionDataKeys.serverConfigId,
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

    if (MLAnalysisType.TIME_SERIES.equals(mlAnalysisType)) {
      Set<String> crypticHostnames = Sets.newHashSet("testNode", "controlNode-1", "controlNode-2", "controlNode-3",
          "controlNode-4", "controlNode-5", "controlNode-6", "controlNode-7");
      if (canaryNewHostNames != null) {
        canaryNewHostNames.removeAll(crypticHostnames);
      }
      if (canaryNewHostNames != null) {
        canaryNewHostNames.removeAll(crypticHostnames);
      }
    }

    putNotNull(executionDetails, "timeDuration",
        ExecutionDataValue.builder().displayName("Analysis duration").value(timeDuration).build());
    putNotNull(executionDetails, "query", ExecutionDataValue.builder().displayName("Query").value(query).build());
    putNotNull(executionDetails, "newVersionNodes",
        ExecutionDataValue.builder().displayName("New version nodes").value(canaryNewHostNames).build());
    putNotNull(executionDetails, "previousVersionNodes",
        ExecutionDataValue.builder().displayName("Previous version nodes").value(lastExecutionNodes).build());
    return executionDetails;
  }
}
