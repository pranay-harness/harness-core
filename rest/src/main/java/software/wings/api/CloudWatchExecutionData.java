package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/9/16.
 */
public class CloudWatchExecutionData extends StateExecutionData {
  private String namespace;
  private String metricName;
  private String percentile;
  private List<Dimension> dimensions = new ArrayList<>();
  private String timeDuration;
  private Datapoint datapoint;
  private String assertionStatement;
  private String assertionStatus;

  /**
   * Gets namespace.
   *
   * @return the namespace
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Sets namespace.
   *
   * @param namespace the namespace
   */
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  /**
   * Gets metric name.
   *
   * @return the metric name
   */
  public String getMetricName() {
    return metricName;
  }

  /**
   * Sets metric name.
   *
   * @param metricName the metric name
   */
  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  /**
   * Gets percentile.
   *
   * @return the percentile
   */
  public String getPercentile() {
    return percentile;
  }

  /**
   * Sets percentile.
   *
   * @param percentile the percentile
   */
  public void setPercentile(String percentile) {
    this.percentile = percentile;
  }

  /**
   * Gets dimensions.
   *
   * @return the dimensions
   */
  public List<Dimension> getDimensions() {
    return dimensions;
  }

  /**
   * Sets dimensions.
   *
   * @param dimensions the dimensions
   */
  public void setDimensions(List<Dimension> dimensions) {
    this.dimensions = dimensions;
  }

  /**
   * Gets time duration.
   *
   * @return the time duration
   */
  public String getTimeDuration() {
    return timeDuration;
  }

  /**
   * Sets time duration.
   *
   * @param timeDuration the time duration
   */
  public void setTimeDuration(String timeDuration) {
    this.timeDuration = timeDuration;
  }

  /**
   * Gets assertion result.
   *
   * @return the assertion result
   */
  public String getAssertionStatus() {
    return assertionStatus;
  }

  /**
   * Sets assertion result.
   *
   * @param assertionStatus the assertion result
   */
  public void setAssertionStatus(String assertionStatus) {
    this.assertionStatus = assertionStatus;
  }

  /**
   * Gets datapoint.
   *
   * @return the datapoint
   */
  public Datapoint getDatapoint() {
    return datapoint;
  }

  /**
   * Sets datapoint.
   *
   * @param datapoint the datapoint
   */
  public void setDatapoint(Datapoint datapoint) {
    this.datapoint = datapoint;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "statistics",
        anExecutionDataValue()
            .withValue(datapoint == null ? null : datapoint.toString())
            .withDisplayName("statistics")
            .build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "namespace",
        anExecutionDataValue().withValue(namespace).withDisplayName("Namespace").build());
    putNotNull(executionDetails, "metricName",
        anExecutionDataValue().withValue(metricName).withDisplayName("metricName").build());
    putNotNull(executionDetails, "percentile",
        anExecutionDataValue().withValue(percentile).withDisplayName("percentile").build());
    putNotNull(executionDetails, "dimensions",
        anExecutionDataValue()
            .withValue(dimensions == null ? null : Lists.transform(dimensions, Functions.toStringFunction()))
            .withDisplayName("dimensions")
            .build());
    putNotNull(executionDetails, "statistics",
        anExecutionDataValue()
            .withValue(datapoint == null ? null : datapoint.toString())
            .withDisplayName("statistics")
            .build());
    putNotNull(executionDetails, "assertion",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
    return executionDetails;
  }

  /**
   * Gets assertion statement.
   *
   * @return the assertion statement
   */
  public String getAssertionStatement() {
    return assertionStatement;
  }

  /**
   * Sets assertion statement.
   *
   * @param assertionStatement the assertion statement
   */
  public void setAssertionStatement(String assertionStatement) {
    this.assertionStatement = assertionStatement;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String stateName;
    private Long startTs;
    private String namespace;
    private Long endTs;
    private String metricName;
    private ExecutionStatus status;
    private String percentile;
    private String errorMsg;
    private List<Dimension> dimensions = new ArrayList<>();
    private String timeDuration;
    private Datapoint datapoint;
    private String assertionStatement;
    private String assertionStatus;

    private Builder() {}

    /**
     * A cloud watch execution data builder.
     *
     * @return the builder
     */
    public static Builder aCloudWatchExecutionData() {
      return new Builder();
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
     * With namespace builder.
     *
     * @param namespace the namespace
     * @return the builder
     */
    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
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
     * With metric name builder.
     *
     * @param metricName the metric name
     * @return the builder
     */
    public Builder withMetricName(String metricName) {
      this.metricName = metricName;
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
     * With percentile builder.
     *
     * @param percentile the percentile
     * @return the builder
     */
    public Builder withPercentile(String percentile) {
      this.percentile = percentile;
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
     * With dimensions builder.
     *
     * @param dimensions the dimensions
     * @return the builder
     */
    public Builder withDimensions(List<Dimension> dimensions) {
      this.dimensions = dimensions;
      return this;
    }

    /**
     * With time duration builder.
     *
     * @param timeDuration the time duration
     * @return the builder
     */
    public Builder withTimeDuration(String timeDuration) {
      this.timeDuration = timeDuration;
      return this;
    }

    /**
     * With datapoint builder.
     *
     * @param datapoint the datapoint
     * @return the builder
     */
    public Builder withDatapoint(Datapoint datapoint) {
      this.datapoint = datapoint;
      return this;
    }

    /**
     * With assertion statement builder.
     *
     * @param assertionStatement the assertion statement
     * @return the builder
     */
    public Builder withAssertionStatement(String assertionStatement) {
      this.assertionStatement = assertionStatement;
      return this;
    }

    /**
     * With assertion status builder.
     *
     * @param assertionStatus the assertion status
     * @return the builder
     */
    public Builder withAssertionStatus(String assertionStatus) {
      this.assertionStatus = assertionStatus;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCloudWatchExecutionData()
          .withStateName(stateName)
          .withStartTs(startTs)
          .withNamespace(namespace)
          .withEndTs(endTs)
          .withMetricName(metricName)
          .withStatus(status)
          .withPercentile(percentile)
          .withErrorMsg(errorMsg)
          .withDimensions(dimensions)
          .withTimeDuration(timeDuration)
          .withDatapoint(datapoint)
          .withAssertionStatement(assertionStatement)
          .withAssertionStatus(assertionStatus);
    }

    /**
     * Build cloud watch execution data.
     *
     * @return the cloud watch execution data
     */
    public CloudWatchExecutionData build() {
      CloudWatchExecutionData cloudWatchExecutionData = new CloudWatchExecutionData();
      cloudWatchExecutionData.setStateName(stateName);
      cloudWatchExecutionData.setStartTs(startTs);
      cloudWatchExecutionData.setNamespace(namespace);
      cloudWatchExecutionData.setEndTs(endTs);
      cloudWatchExecutionData.setMetricName(metricName);
      cloudWatchExecutionData.setStatus(status);
      cloudWatchExecutionData.setPercentile(percentile);
      cloudWatchExecutionData.setErrorMsg(errorMsg);
      cloudWatchExecutionData.setDimensions(dimensions);
      cloudWatchExecutionData.setTimeDuration(timeDuration);
      cloudWatchExecutionData.setDatapoint(datapoint);
      cloudWatchExecutionData.setAssertionStatement(assertionStatement);
      cloudWatchExecutionData.setAssertionStatus(assertionStatus);
      return cloudWatchExecutionData;
    }
  }
}
