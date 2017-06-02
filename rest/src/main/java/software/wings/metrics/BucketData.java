package software.wings.metrics;

import com.google.common.math.Stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mongodb.morphia.annotations.Embedded;
import java.util.List;

/**
 * Created by mike@ on 4/11/17.
 */
public class BucketData {
  private RiskLevel risk;
  @Embedded private DataSummary oldData;
  @Embedded private DataSummary newData;

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
    private RiskLevel risk;
    private DataSummary oldData;
    private DataSummary newData;

    private Builder() {}

    public static Builder aBucketData() {
      return new Builder();
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
      return aBucketData().withRisk(risk).withOldData(oldData).withNewData(newData);
    }

    public BucketData build() {
      BucketData bucketData = new BucketData();
      bucketData.setRisk(risk);
      bucketData.setOldData(oldData);
      bucketData.setNewData(newData);
      return bucketData;
    }
  }
}
