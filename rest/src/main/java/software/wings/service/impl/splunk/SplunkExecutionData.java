package software.wings.service.impl.splunk;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.CountsByStatuses;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 8/4/16.
 */
public class SplunkExecutionData extends StateExecutionData {
  private String correlationId;
  private String stateExecutionInstanceId;
  private String splunkConfigId;
  private Set<String> queries;
  private int timeDuration;

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public String getSplunkConfigId() {
    return splunkConfigId;
  }

  public void setSplunkConfigId(String splunkConfigId) {
    this.splunkConfigId = splunkConfigId;
  }

  public Set<String> getQueries() {
    return queries;
  }

  public void setQueries(Set<String> queries) {
    this.queries = queries;
  }

  public int getTimeDuration() {
    return timeDuration;
  }

  public void setTimeDuration(int timeDuration) {
    this.timeDuration = timeDuration;
  }

  @Override
  public String toString() {
    return "SplunkExecutionData{"
        + "correlationId='" + correlationId + '\'' + ", stateExecutionInstanceId='" + stateExecutionInstanceId + '\''
        + ", splunkConfigId='" + splunkConfigId + '\'' + ", queries=" + queries + ", timeDuration=" + timeDuration
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof SplunkExecutionData))
      return false;

    SplunkExecutionData that = (SplunkExecutionData) o;

    if (timeDuration != that.timeDuration)
      return false;
    if (correlationId != null ? !correlationId.equals(that.correlationId) : that.correlationId != null)
      return false;
    if (stateExecutionInstanceId != null ? !stateExecutionInstanceId.equals(that.stateExecutionInstanceId)
                                         : that.stateExecutionInstanceId != null)
      return false;
    if (splunkConfigId != null ? !splunkConfigId.equals(that.splunkConfigId) : that.splunkConfigId != null)
      return false;
    return queries != null ? queries.equals(that.queries) : that.queries == null;
  }

  @Override
  public int hashCode() {
    int result = correlationId != null ? correlationId.hashCode() : 0;
    result = 31 * result + (stateExecutionInstanceId != null ? stateExecutionInstanceId.hashCode() : 0);
    result = 31 * result + (splunkConfigId != null ? splunkConfigId.hashCode() : 0);
    result = 31 * result + (queries != null ? queries.hashCode() : 0);
    result = 31 * result + timeDuration;
    return result;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = getExecutionDetails();
    putNotNull(executionDetails, "stateExecutionInstanceId",
        anExecutionDataValue().withValue(stateExecutionInstanceId).withDisplayName("State Execution Id").build());
    putNotNull(executionDetails, "splunkConfigId",
        anExecutionDataValue().withValue(splunkConfigId).withDisplayName("Splunk Config Id").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "errorMsg",
        anExecutionDataValue().withValue(getErrorMsg()).withDisplayName("Message").build());
    putNotNull(executionDetails, "total",
        anExecutionDataValue()
            .withDisplayName("Total")
            .withValue(timeDuration + SplunkDataCollectionTask.DELAY_MINUTES + 1)
            .build());
    final CountsByStatuses breakdown = new CountsByStatuses();
    breakdown.setSuccess((int) TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - getStartTs()));
    putNotNull(executionDetails, "breakdown",
        anExecutionDataValue().withDisplayName("breakdown").withValue(breakdown).build());
    putNotNull(executionDetails, "timeDuration",
        anExecutionDataValue().withValue(timeDuration).withDisplayName("Analysis duration").build());
    putNotNull(executionDetails, "queries",
        anExecutionDataValue().withValue(queries).withDisplayName("Splunk queries").build());
    return executionDetails;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String correlationId;
    private String stateExecutionInstanceId;
    private String splunkConfigId;
    private Set<String> queries;
    private int timeDuration;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private Builder() {}

    /**
     * An splunk execution data builder.
     *
     * @return the builder
     */
    public static Builder anSplunkExecutionData() {
      return new Builder();
    }

    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public Builder withCorrelationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder withSplunkConfigID(String splunkConfigId) {
      this.splunkConfigId = splunkConfigId;
      return this;
    }

    public Builder withSplunkQueries(Set<String> queries) {
      this.queries = queries;
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

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anSplunkExecutionData()
          .withCorrelationId(correlationId)
          .withSplunkConfigID(splunkConfigId)
          .withSplunkQueries(queries)
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
    public SplunkExecutionData build() {
      SplunkExecutionData splunkExecutionData = new SplunkExecutionData();
      splunkExecutionData.setCorrelationId(correlationId);
      splunkExecutionData.setStateExecutionInstanceId(stateExecutionInstanceId);
      splunkExecutionData.setSplunkConfigId(splunkConfigId);
      splunkExecutionData.setQueries(queries);
      splunkExecutionData.setTimeDuration(timeDuration);
      splunkExecutionData.setStateName(stateName);
      splunkExecutionData.setStartTs(startTs);
      splunkExecutionData.setEndTs(endTs);
      splunkExecutionData.setStatus(status);
      splunkExecutionData.setErrorMsg(errorMsg);
      return splunkExecutionData;
    }
  }
}
