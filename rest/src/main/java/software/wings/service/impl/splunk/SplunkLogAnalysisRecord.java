package software.wings.service.impl.splunk;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

/**
 * Created by rsingh on 6/23/17.
 */
@Entity(value = "splunkAnalysisRecords", noClassnameStored = true)
public class SplunkLogAnalysisRecord extends Base {
  @NotEmpty @Indexed private String stateExecutionInstanceId;
  @NotEmpty private String logMessage;
  @NotEmpty private String splunkMlVersion;
  private float riskScore;
  private int count;
  private int anomalyCountLabel;
  private int anomalyLabel;
  private int clusterId;
  @Indexed private long timestamp;

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public String getLogMessage() {
    return logMessage;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  public String getSplunkMlVersion() {
    return splunkMlVersion;
  }

  public void setSplunkMlVersion(String splunkMlVersion) {
    this.splunkMlVersion = splunkMlVersion;
  }

  public float getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(float riskScore) {
    this.riskScore = riskScore;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getAnomalyCountLabel() {
    return anomalyCountLabel;
  }

  public void setAnomalyCountLabel(int anomalyCountLabel) {
    this.anomalyCountLabel = anomalyCountLabel;
  }

  public int getAnomalyLabel() {
    return anomalyLabel;
  }

  public void setAnomalyLabel(int anomalyLabel) {
    this.anomalyLabel = anomalyLabel;
  }

  public int getClusterId() {
    return clusterId;
  }

  public void setClusterId(int clusterId) {
    this.clusterId = clusterId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return "SplunkLogAnalysisRecord{"
        + "stateExecutionInstanceId='" + stateExecutionInstanceId + '\'' + ", logMessage='" + logMessage + '\''
        + ", splunkMlVersion='" + splunkMlVersion + '\'' + ", riskScore=" + riskScore + ", count=" + count
        + ", anomalyCountLabel=" + anomalyCountLabel + ", anomalyLabel=" + anomalyLabel + ", clusterId=" + clusterId
        + ", timestamp=" + timestamp + "} " + super.toString();
  }
}
