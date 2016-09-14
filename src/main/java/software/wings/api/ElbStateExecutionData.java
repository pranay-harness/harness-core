package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.base.MoreObjects;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 9/12/16.
 */
public class ElbStateExecutionData extends StateExecutionData {
  private String hostName;

  /**
   * Getter for property 'hostName'.
   *
   * @return Value for property 'hostName'.
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Setter for property 'hostName'.
   *
   * @param hostName Value to set for property 'hostName'.
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostName", hostName).toString();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> data = super.getExecutionSummary();
    putNotNull(data, "hostName", anExecutionDataValue().withDisplayName("Host").withValue(hostName).build());
    return data;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> data = super.getExecutionDetails();
    putNotNull(data, "hostName", anExecutionDataValue().withDisplayName("Host").withValue(hostName).build());
    return data;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Host name.
     */
    String hostName;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private Builder() {}

    /**
     * An elb state execution data builder.
     *
     * @return the builder
     */
    public static Builder anElbStateExecutionData() {
      return new Builder();
    }

    /**
     * With host name builder.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
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
      return anElbStateExecutionData()
          .withHostName(hostName)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    /**
     * Build elb state execution data.
     *
     * @return the elb state execution data
     */
    public ElbStateExecutionData build() {
      ElbStateExecutionData eLBStateExecutionData = new ElbStateExecutionData();
      eLBStateExecutionData.setHostName(hostName);
      eLBStateExecutionData.setStateName(stateName);
      eLBStateExecutionData.setStartTs(startTs);
      eLBStateExecutionData.setEndTs(endTs);
      eLBStateExecutionData.setStatus(status);
      eLBStateExecutionData.setErrorMsg(errorMsg);
      return eLBStateExecutionData;
    }
  }
}
