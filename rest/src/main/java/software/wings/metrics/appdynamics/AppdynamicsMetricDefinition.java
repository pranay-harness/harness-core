package software.wings.metrics.appdynamics;

import com.google.common.base.MoreObjects;

import software.wings.metrics.MetricDefinition;
import software.wings.metrics.MetricType;
import software.wings.metrics.ThresholdComparisonType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike@ on 5/23/17.
 */
public class AppdynamicsMetricDefinition extends MetricDefinition {
  private long appdynamicsAppId;

  public long getAppdynamicsAppId() {
    return appdynamicsAppId;
  }

  public void setAppdynamicsAppId(long appdynamicsAppId) {
    this.appdynamicsAppId = appdynamicsAppId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    AppdynamicsMetricDefinition that = (AppdynamicsMetricDefinition) o;

    return appdynamicsAppId == that.appdynamicsAppId;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (int) (appdynamicsAppId ^ (appdynamicsAppId >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("metricName", metricName)
        .add("metricType", metricType)
        .add("thresholds", thresholds)
        .add("appdynamicsAppId", appdynamicsAppId)
        .add("metricId", metricId)
        .toString();
  }

  public static final class Builder {
    private String accountId;
    private String metricId;
    private String metricName;
    private MetricType metricType;
    private Map<ThresholdComparisonType, Threshold> thresholds = new HashMap<>();
    private long appdynamicsAppId;

    private Builder() {}

    /**
     * an AppdynamicsMetricDefinition base builder.
     *
     * @return the builder
     */
    public static Builder anAppdynamicsMetricDefinition() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withMetricId(String metricId) {
      this.metricId = metricId;
      return this;
    }

    public Builder withMetricName(String metricName) {
      this.metricName = metricName;
      return this;
    }

    public Builder withMetricType(MetricType metricType) {
      this.metricType = metricType;
      return this;
    }

    public Builder withThresholds(Map<ThresholdComparisonType, Threshold> thresholds) {
      this.thresholds = thresholds;
      return this;
    }

    public Builder withThreshold(ThresholdComparisonType tct, Threshold threshold) {
      this.thresholds.put(tct, threshold);
      return this;
    }

    public Builder withAppdynamicsAppId(long appdynamicsAppId) {
      this.appdynamicsAppId = appdynamicsAppId;
      return this;
    }

    public Builder but() {
      return anAppdynamicsMetricDefinition()
          .withAccountId(accountId)
          .withMetricName(metricName)
          .withMetricType(metricType)
          .withThresholds(thresholds)
          .withAppdynamicsAppId(appdynamicsAppId)
          .withMetricId(metricId);
    }

    public AppdynamicsMetricDefinition build() {
      AppdynamicsMetricDefinition appdynamicsMetricDefinition = new AppdynamicsMetricDefinition();
      appdynamicsMetricDefinition.setAccountId(accountId);
      appdynamicsMetricDefinition.setMetricName(metricName);
      appdynamicsMetricDefinition.setMetricType(metricType);
      appdynamicsMetricDefinition.setThresholds(thresholds);
      appdynamicsMetricDefinition.setAppdynamicsAppId(appdynamicsAppId);
      appdynamicsMetricDefinition.setMetricId(metricId);
      return appdynamicsMetricDefinition;
    }
  }
}
