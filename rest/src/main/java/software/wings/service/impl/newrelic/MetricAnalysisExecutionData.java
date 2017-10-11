package software.wings.service.impl.newrelic;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import lombok.Data;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.CountsByStatuses;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/4/16.
 */
@Data
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
        anExecutionDataValue().withValue(stateExecutionInstanceId).withDisplayName("State Execution Id").build());
    putNotNull(executionDetails, "serverConfigId",
        anExecutionDataValue().withValue(serverConfigId).withDisplayName("Server Config Id").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "errorMsg",
        anExecutionDataValue().withValue(getErrorMsg()).withDisplayName("Message").build());
    final int total = timeDuration;
    putNotNull(executionDetails, "total", anExecutionDataValue().withDisplayName("Total").withValue(total).build());
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
    putNotNull(executionDetails, "breakdown",
        anExecutionDataValue().withDisplayName("breakdown").withValue(breakdown).build());
    putNotNull(executionDetails, "timeDuration",
        anExecutionDataValue().withValue(timeDuration).withDisplayName("Analysis duration").build());
    return executionDetails;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String correlationId;
    private String workflowExecutionId;
    private String stateExecutionInstanceId;
    private String serverConfigId;
    private int timeDuration;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;
    private Set<String> canaryNewHostNames;
    private Set<String> lastExecutionNodes;

    private Builder() {}

    public static Builder anAnanlysisExecutionData() {
      return new Builder();
    }

    public Builder withWorkflowExecutionId(String workflowExecutionId) {
      this.workflowExecutionId = workflowExecutionId;
      return this;
    }

    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public Builder withCorrelationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder withServerConfigID(String serverConfigId) {
      this.serverConfigId = serverConfigId;
      return this;
    }

    public Builder withAnalysisDuration(int timeDuration) {
      this.timeDuration = timeDuration;
      return this;
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With error msg builder.
     *
     * @param errorMsg the error msg
     * @return the builder
     */
    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public Builder withCanaryNewHostNames(Set<String> canaryNewHostNames) {
      this.canaryNewHostNames = canaryNewHostNames;
      return this;
    }

    public Builder withLastExecutionNodes(Set<String> lastExecutionNodes) {
      this.lastExecutionNodes = lastExecutionNodes;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAnanlysisExecutionData()
          .withCorrelationId(correlationId)
          .withServerConfigID(serverConfigId)
          .withAnalysisDuration(timeDuration)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    /**
     * Build app dynamics execution data.
     *
     * @return the app dynamics execution data
     */
    public MetricAnalysisExecutionData build() {
      MetricAnalysisExecutionData metricAnalysisExecutionData = new MetricAnalysisExecutionData();
      metricAnalysisExecutionData.setCorrelationId(correlationId);
      metricAnalysisExecutionData.setWorkflowExecutionId(workflowExecutionId);
      metricAnalysisExecutionData.setStateExecutionInstanceId(stateExecutionInstanceId);
      metricAnalysisExecutionData.setServerConfigId(serverConfigId);
      metricAnalysisExecutionData.setTimeDuration(timeDuration);
      metricAnalysisExecutionData.setStateName(stateName);
      metricAnalysisExecutionData.setStartTs(startTs);
      metricAnalysisExecutionData.setEndTs(endTs);
      metricAnalysisExecutionData.setStatus(status);
      metricAnalysisExecutionData.setErrorMsg(errorMsg);
      metricAnalysisExecutionData.setCanaryNewHostNames(canaryNewHostNames);
      metricAnalysisExecutionData.setLastExecutionNodes(lastExecutionNodes);
      return metricAnalysisExecutionData;
    }
  }
}
