package software.wings.metrics.appdynamics;

import com.google.common.base.MoreObjects;

import software.wings.metrics.MetricDefinition;
import software.wings.metrics.MetricType;

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
        .add("mediumThreshold", mediumThreshold)
        .add("highThreshold", highThreshold)
        .add("thresholdType", thresholdType)
        .add("appdynamicsAppId", appdynamicsAppId)
        .add("metricId", metricId)
        .toString();
  }

  public static final class Builder {
    private String accountId;
    private String metricId;
    private String metricName;
    private MetricType metricType;
    private double mediumThreshold;
    private double highThreshold;
    private ThresholdType thresholdType;
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

    public Builder withMediumThreshold(double threshold) {
      this.mediumThreshold = threshold;
      return this;
    }

    public Builder withHighThreshold(double threshold) {
      this.highThreshold = threshold;
      return this;
    }

    public Builder withThresholdType(ThresholdType thresholdType) {
      this.thresholdType = thresholdType;
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
          .withMediumThreshold(mediumThreshold)
          .withHighThreshold(highThreshold)
          .withThresholdType(thresholdType)
          .withAppdynamicsAppId(appdynamicsAppId)
          .withMetricId(metricId);
    }

    public AppdynamicsMetricDefinition build() {
      AppdynamicsMetricDefinition appdynamicsMetricDefinition = new AppdynamicsMetricDefinition();
      appdynamicsMetricDefinition.setAccountId(accountId);
      appdynamicsMetricDefinition.setMetricName(metricName);
      appdynamicsMetricDefinition.setMetricType(metricType);
      appdynamicsMetricDefinition.setMediumThreshold(mediumThreshold);
      appdynamicsMetricDefinition.setHighThreshold(highThreshold);
      appdynamicsMetricDefinition.setThresholdType(thresholdType);
      appdynamicsMetricDefinition.setAppdynamicsAppId(appdynamicsAppId);
      appdynamicsMetricDefinition.setMetricId(metricId);
      return appdynamicsMetricDefinition;
    }
  }
}
