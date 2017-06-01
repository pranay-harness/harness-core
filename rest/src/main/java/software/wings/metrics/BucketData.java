package software.wings.metrics;

import com.google.common.math.Stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

import java.util.Date;
import java.util.List;

/**
 * Created by mike@ on 4/11/17.
 */
@Entity(value = "completedMetricsSummary", noClassnameStored = true)
public class BucketData extends Base {
  @Indexed private String accountId;
  @Indexed private String stateExecutionInstanceId;
  private String btName;
  private String metricName;
  private String btId;
  @Indexed private String metricId;
  private long startTimeMillis;
  private long endTimeMillis;
  private RiskLevel risk;
  @Embedded private DataSummary oldData;
  @Embedded private DataSummary newData;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public long getStartTimeMillis() {
    return startTimeMillis;
  }

  public void setStartTimeMillis(long startTimeMillis) {
    this.startTimeMillis = startTimeMillis;
  }

  public long getEndTimeMillis() {
    return endTimeMillis;
  }

  public void setEndTimeMillis(long endTimeMillis) {
    this.endTimeMillis = endTimeMillis;
  }

  public RiskLevel getRisk() {
    return risk;
  }

  public void setRisk(RiskLevel risk) {
    this.risk = risk;
  }

  public DataSummary getOldData() {
    return oldData;
  }

  public void setOldData(DataSummary summary) {
    this.oldData = summary;
  }

  public DataSummary getNewData() {
    return newData;
  }

  public void setNewData(DataSummary summary) {
    this.newData = summary;
  }

  public String getBtName() {
    return btName;
  }

  public void setBtName(String btName) {
    this.btName = btName;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public String getBtId() {
    return btId;
  }

  public void setBtId(String btId) {
    this.btId = btId;
  }

  public String getMetricId() {
    return metricId;
  }

  public void setMetricId(String metricId) {
    this.metricId = metricId;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("Account: ").append(accountId).append("\n");
    s.append("State Execution Instance: ").append(stateExecutionInstanceId).append("\n");
    s.append("BT: ").append(btName).append("\n");
    s.append("Metric: ").append(metricName).append("\n");
    s.append("Start: ").append(startTimeMillis).append(" (").append(new Date(startTimeMillis).toString()).append(")\n");
    s.append("End: ").append(endTimeMillis).append(" (").append(new Date(endTimeMillis).toString()).append(")\n");
    s.append("Risk: ").append(risk.name()).append("\n");
    s.append("OldData: ").append(oldData).append("\n");
    s.append("NewData: ").append(newData).append("\n");
    return s.toString();
  }

  public class DataSummary {
    private int nodeCount;
    private List<String> nodeList;
    private Stats stats;
    private String displayValue;
    private boolean missingData;

    // needed for Jackson
    public DataSummary() {}

    public DataSummary(int nodeCount, List<String> nodeList, Stats stats, String displayValue, boolean missingData) {
      this.nodeCount = nodeCount;
      this.nodeList = nodeList;
      this.stats = stats;
      this.displayValue = displayValue;
      this.missingData = missingData;
    }

    public int getNodeCount() {
      return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
      this.nodeCount = nodeCount;
    }

    public List<String> getNodeList() {
      return nodeList;
    }

    public void setNodeList(List<String> nodeList) {
      this.nodeList = nodeList;
    }

    @JsonIgnore
    public Stats getStats() {
      return stats;
    }

    public void setStats(Stats stats) {
      this.stats = stats;
    }

    public String getDisplayValue() {
      return displayValue;
    }

    public void setDisplayValue(String displayValue) {
      this.displayValue = displayValue;
    }

    public boolean isMissingData() {
      return missingData;
    }

    public void setMissingData(boolean missingData) {
      this.missingData = missingData;
    }

    @Override
    public String toString() {
      return "DataSummary{"
          + "nodeCount=" + nodeCount + ", nodeList=" + nodeList + ", stats=" + stats + ", displayValue='" + displayValue
          + '\'' + ", missingData=" + missingData + '}';
    }
  }

  public static final class Builder {
    private String accountId;
    private String stateExecutionInstanceId;
    private String btName;
    private String metricName;
    private String btId;
    private String metricId;
    private long startTimeMillis;
    private long endTimeMillis;
    private RiskLevel risk;
    private DataSummary oldData;
    private DataSummary newData;

    private Builder() {}

    public static Builder aBucketData() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public Builder withBtName(String btName) {
      this.btName = btName;
      return this;
    }

    public Builder withBtId(String btId) {
      this.btId = btId;
      return this;
    }

    public Builder withMetricName(String metricName) {
      this.metricName = metricName;
      return this;
    }

    public Builder withMetricId(String metricId) {
      this.metricId = metricId;
      return this;
    }

    public Builder withStartTimeMillis(long startTimeMillis) {
      this.startTimeMillis = startTimeMillis;
      return this;
    }

    public Builder withEndTimeMillis(long endTimeMillis) {
      this.endTimeMillis = endTimeMillis;
      return this;
    }

    public Builder withRisk(RiskLevel risk) {
      this.risk = risk;
      return this;
    }

    public Builder withOldData(DataSummary oldData) {
      this.oldData = oldData;
      return this;
    }

    public Builder withNewData(DataSummary newData) {
      this.newData = newData;
      return this;
    }

    public Builder but() {
      return aBucketData()
          .withAccountId(accountId)
          .withStateExecutionInstanceId(stateExecutionInstanceId)
          .withBtName(btName)
          .withBtId(btId)
          .withMetricName(metricName)
          .withMetricId(metricId)
          .withStartTimeMillis(startTimeMillis)
          .withEndTimeMillis(endTimeMillis)
          .withRisk(risk)
          .withOldData(oldData)
          .withNewData(newData);
    }

    public BucketData build() {
      BucketData bucketData = new BucketData();
      bucketData.setAccountId(accountId);
      bucketData.setStateExecutionInstanceId(stateExecutionInstanceId);
      bucketData.setBtName(btName);
      bucketData.setBtId(btId);
      bucketData.setMetricName(metricName);
      bucketData.setMetricId(metricId);
      bucketData.setStartTimeMillis(startTimeMillis);
      bucketData.setEndTimeMillis(endTimeMillis);
      bucketData.setRisk(risk);
      bucketData.setOldData(oldData);
      bucketData.setNewData(newData);
      return bucketData;
    }
  }
}
