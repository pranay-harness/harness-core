package software.wings.metrics;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

/**
 * Created by mike@ on 5/23/17.
 */
@Entity(value = "appdynamicsMetrics", noClassnameStored = true)
public abstract class MetricDefinition extends Base {
  @NotEmpty protected String accountId;
  @NotEmpty protected String metricId;
  @NotEmpty protected String metricName;
  @NotEmpty protected MetricType metricType;
  protected double mediumThreshold;
  protected double highThreshold;
  @NotEmpty protected ThresholdType thresholdType;

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

  public double getMediumThreshold() {
    return mediumThreshold;
  }

  public void setMediumThreshold(double threshold) {
    this.mediumThreshold = threshold;
  }

  public double getHighThreshold() {
    return highThreshold;
  }

  public void setHighThreshold(double threshold) {
    this.highThreshold = threshold;
  }

  public ThresholdType getThresholdType() {
    return thresholdType;
  }

  public void setThresholdType(ThresholdType thresholdType) {
    this.thresholdType = thresholdType;
  }

  public enum ThresholdType {
    ALERT_WHEN_LOWER,
    ALERT_WHEN_HIGHER,
    NO_ALERT,
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

    if (Double.compare(that.mediumThreshold, mediumThreshold) != 0)
      return false;
    if (Double.compare(that.highThreshold, highThreshold) != 0)
      return false;
    if (!accountId.equals(that.accountId))
      return false;
    if (!metricId.equals(that.metricId))
      return false;
    if (!metricName.equals(that.metricName))
      return false;
    if (metricType != that.metricType)
      return false;
    return thresholdType == that.thresholdType;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    long temp;
    result = 31 * result + accountId.hashCode();
    result = 31 * result + metricId.hashCode();
    result = 31 * result + metricName.hashCode();
    result = 31 * result + metricType.hashCode();
    temp = Double.doubleToLongBits(mediumThreshold);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(highThreshold);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + thresholdType.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("metricId", metricId)
        .add("metricName", metricName)
        .add("metricType", metricType)
        .add("mediumThreshold", mediumThreshold)
        .add("highThreshold", highThreshold)
        .add("thresholdType", thresholdType)
        .toString();
  }
}
