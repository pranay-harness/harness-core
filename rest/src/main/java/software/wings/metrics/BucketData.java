package software.wings.metrics;

import com.google.common.math.Stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by mike@ on 4/11/17.
 */
public class BucketData {
  private long startTimeMillis;
  private long endTimeMillis;
  private RiskLevel risk;
  private DataSummary oldData;
  private DataSummary newData;

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

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("Start: ").append(startTimeMillis).append(" (").append(new Date(startTimeMillis).toString()).append(")\n");
    s.append("End: ").append(endTimeMillis).append(" (").append(new Date(endTimeMillis).toString()).append(")\n");
    s.append("Risk: ").append(risk.name()).append("\n");
    s.append("OldData: ").append(oldData).append("\n");
    s.append("NewData: ").append(newData).append("\n");
    return s.toString();
  }

  public class DataSummary {
    private int nodeCount;
    private Stats stats;
    private String displayValue;
    private boolean missingData;

    public DataSummary(int nodeCount, Stats stats, String displayValue, boolean missingData) {
      this.nodeCount = nodeCount;
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
          + "nodeCount=" + nodeCount + ", stats=" + stats + ", displayValue='" + displayValue + '\''
          + ", missingData=" + missingData + '}';
    }
  }

  public static final class Builder {
    private long startTimeMillis;
    private long endTimeMillis;
    private RiskLevel risk;
    private DataSummary oldData;
    private DataSummary newData;

    private Builder() {}

    public static Builder aBucketData() {
      return new Builder();
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
          .withStartTimeMillis(startTimeMillis)
          .withEndTimeMillis(endTimeMillis)
          .withRisk(risk)
          .withOldData(oldData)
          .withNewData(newData);
    }

    public BucketData build() {
      BucketData bucketData = new BucketData();
      bucketData.setStartTimeMillis(startTimeMillis);
      bucketData.setEndTimeMillis(endTimeMillis);
      bucketData.setRisk(risk);
      bucketData.setOldData(oldData);
      bucketData.setNewData(newData);
      return bucketData;
    }
  }
}
