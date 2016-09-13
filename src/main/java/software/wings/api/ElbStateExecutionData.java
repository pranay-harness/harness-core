package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.base.MoreObjects;

import com.amazonaws.AmazonWebServiceResult;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 9/12/16.
 */
public class ElbStateExecutionData extends StateExecutionData {
  private AmazonWebServiceResult amazonWebServiceResult;
  private String hostName;

  /**
   * Getter for property 'amazonWebServiceResult'.
   *
   * @return Value for property 'amazonWebServiceResult'.
   */
  public AmazonWebServiceResult getAmazonWebServiceResult() {
    return amazonWebServiceResult;
  }

  /**
   * Setter for property 'amazonWebServiceResult'.
   *
   * @param amazonWebServiceResult Value to set for property 'amazonWebServiceResult'.
   */
  public void setAmazonWebServiceResult(AmazonWebServiceResult amazonWebServiceResult) {
    this.amazonWebServiceResult = amazonWebServiceResult;
  }

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
    return MoreObjects.toStringHelper(this)
        .add("amazonWebServiceResult", amazonWebServiceResult)
        .add("hostName", hostName)
        .toString();
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

  public static final class Builder {
    AmazonWebServiceResult amazonWebServiceResult;
    String hostName;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private Builder() {}

    public static Builder anElbStateExecutionData() {
      return new Builder();
    }

    public Builder withAmazonWebServiceResult(AmazonWebServiceResult amazonWebServiceResult) {
      this.amazonWebServiceResult = amazonWebServiceResult;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public Builder but() {
      return anElbStateExecutionData()
          .withAmazonWebServiceResult(amazonWebServiceResult)
          .withHostName(hostName)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    public ElbStateExecutionData build() {
      ElbStateExecutionData eLBStateExecutionData = new ElbStateExecutionData();
      eLBStateExecutionData.setAmazonWebServiceResult(amazonWebServiceResult);
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
