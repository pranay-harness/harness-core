package software.wings.metrics;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

import java.util.Map;

/**
 * Created by mike@ on 5/23/17.
 */
@Entity(value = "appdynamicsMetricDefinitions", noClassnameStored = true)
public abstract class MetricDefinition extends Base {
  @NotEmpty protected String accountId;
  @NotEmpty protected String metricId;
  @NotEmpty protected String metricName;
  @NotEmpty protected MetricType metricType;
  protected Map<ThresholdComparisonType, Threshold> thresholds;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getMetricId() {
    return metricId;
  }

  public void setMetricId(String metricId) {
    this.metricId = metricId;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }

  public Map<ThresholdComparisonType, Threshold> getThresholds() {
    return thresholds;
  }

  public void setThresholds(Map<ThresholdComparisonType, Threshold> thresholds) {
    this.thresholds = thresholds;
  }

  public void addThreshold(ThresholdComparisonType tct, Threshold threshold) {
    thresholds.put(tct, threshold);
  }

  public enum ThresholdType {
    ALERT_WHEN_LOWER,
    ALERT_WHEN_HIGHER,
    NO_ALERT,
  }

  public static class Threshold {
    private ThresholdType thresholdType;
    private double high;
    private double medium;

    // needed for Jackson
    public Threshold() {}

    public Threshold(ThresholdType thresholdType, double medium, double high) {
      this.thresholdType = thresholdType;
      this.medium = medium;
      this.high = high;
    }

    public ThresholdType getThresholdType() {
      return thresholdType;
    }

    public void setThresholdType(ThresholdType thresholdType) {
      this.thresholdType = thresholdType;
    }

    public double getHigh() {
      return high;
    }

    public void setHigh(double high) {
      this.high = high;
    }

    public double getMedium() {
      return medium;
    }

    public void setMedium(double medium) {
      this.medium = medium;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("thresholdType", thresholdType)
          .add("high", high)
          .add("medium", medium)
          .toString();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    MetricDefinition that = (MetricDefinition) o;

    if (!accountId.equals(that.accountId))
      return false;
    if (!metricId.equals(that.metricId))
      return false;
    if (!metricName.equals(that.metricName))
      return false;
    if (metricType != that.metricType)
      return false;
    return thresholds != null ? thresholds.equals(that.thresholds) : that.thresholds == null;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + accountId.hashCode();
    result = 31 * result + metricId.hashCode();
    result = 31 * result + metricName.hashCode();
    result = 31 * result + metricType.hashCode();
    result = 31 * result + (thresholds != null ? thresholds.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("metricId", metricId)
        .add("metricName", metricName)
        .add("metricType", metricType)
        .add("thresholds", thresholds)
        .toString();
  }
}
