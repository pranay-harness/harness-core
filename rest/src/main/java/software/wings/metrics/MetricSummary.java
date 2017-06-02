package software.wings.metrics;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by mike@ on 6/1/17.
 */
@Entity(value = "metricSummary", noClassnameStored = true)
public class MetricSummary extends Base {
  @Indexed private String accountId;
  @Indexed private String stateExecutionInstanceId;
  private Map<String, BTMetrics> btMetricsMap;
  private long startTimeMillis;
  private long endTimeMillis;

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

  public Map<String, BTMetrics> getBtMetricsMap() {
    return btMetricsMap;
  }

  public void setBtMetricsMap(Map<String, BTMetrics> btMetricsMap) {
    this.btMetricsMap = btMetricsMap;
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

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("Account: ").append(accountId).append("\n");
    s.append("State Execution Instance: ").append(stateExecutionInstanceId).append("\n");
    s.append("Start: ").append(startTimeMillis).append(" (").append(new Date(startTimeMillis).toString()).append(")\n");
    s.append("End: ").append(endTimeMillis).append(" (").append(new Date(endTimeMillis).toString()).append(")\n");
    s.append("BTMetrics: ").append(btMetricsMap.toString()).append("\n");
    return s.toString();
  }

  public class Metric {
    private String metricName;
    private String metricId;

    public Metric() {}

    public Metric(String metricName, String metricId) {
      this.metricName = metricName;
      this.metricId = metricId;
    }

    public String getMetricName() {
      return metricName;
    }

    public void setMetricName(String metricName) {
      this.metricName = metricName;
    }

    public String getMetricId() {
      return metricId;
    }

    public void setMetricId(String metricId) {
      this.metricId = metricId;
    }

    @Override
    //    public String toString() {
    //      return MoreObjects.toStringHelper(this).add("metricName", metricName).add("metricId", metricId).toString();
    //    }
    public String toString() {
      return metricName;
    }
  }

  public class BTMetrics {
    private RiskLevel btRisk;
    private List<String> btRiskSummary;
    private Map<Metric, BucketData> metricsMap;

    // needed for Jackson
    public BTMetrics() {}

    public BTMetrics(RiskLevel btRisk, List<String> btRiskSummary, Map<Metric, BucketData> metricsMap) {
      this.btRisk = btRisk;
      this.btRiskSummary = btRiskSummary;
      this.metricsMap = metricsMap;
    }

    public RiskLevel getBtRisk() {
      return btRisk;
    }

    public void setBtRisk(RiskLevel btRisk) {
      this.btRisk = btRisk;
    }

    public List<String> getBtRiskSummary() {
      return btRiskSummary;
    }

    public void setBtRiskSummary(List<String> btRiskSummary) {
      this.btRiskSummary = btRiskSummary;
    }

    public Map<Metric, BucketData> getMetricsMap() {
      return metricsMap;
    }

    public void setMetricsMap(Map<Metric, BucketData> metricsMap) {
      this.metricsMap = metricsMap;
    }

    public BucketData getMetricBucketData(String metric) {
      Optional<Metric> optionalMetric =
          metricsMap.keySet().stream().filter(m -> m.getMetricName().equalsIgnoreCase(metric)).findFirst();
      if (optionalMetric.isPresent()) {
        return metricsMap.get(optionalMetric.get());
      }
      return null;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("btRisk", btRisk)
          .add("btRiskSummary", btRiskSummary)
          .add("metricsMap", metricsMap)
          .toString();
    }
  }

  public static final class Builder {
    private String accountId;
    private String stateExecutionInstanceId;
    private Map<String, BTMetrics> btMetricsMap;
    private long startTimeMillis;
    private long endTimeMillis;

    private Builder() {}

    public static Builder aMetricSummary() {
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

    public Builder withBtMetricsMap(Map<String, BTMetrics> btMetricsMap) {
      this.btMetricsMap = btMetricsMap;
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

    public Builder but() {
      return aMetricSummary()
          .withAccountId(accountId)
          .withStateExecutionInstanceId(stateExecutionInstanceId)
          .withBtMetricsMap(btMetricsMap)
          .withStartTimeMillis(startTimeMillis)
          .withEndTimeMillis(endTimeMillis);
    }

    public MetricSummary build() {
      MetricSummary metricSummary = new MetricSummary();
      metricSummary.setAccountId(accountId);
      metricSummary.setStateExecutionInstanceId(stateExecutionInstanceId);
      metricSummary.setBtMetricsMap(btMetricsMap);
      metricSummary.setStartTimeMillis(startTimeMillis);
      metricSummary.setEndTimeMillis(endTimeMillis);
      return metricSummary;
    }
  }
}
