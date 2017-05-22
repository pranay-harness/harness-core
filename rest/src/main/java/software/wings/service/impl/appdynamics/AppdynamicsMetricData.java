package software.wings.service.impl.appdynamics;

import java.util.Arrays;

/**
 * Created by rsingh on 5/17/17.
 */
public class AppdynamicsMetricData {
  private String metricName;
  private long metricId;
  private String metricPath;
  private String frequency;
  private AppdynamicsMetricDataValue[] metricValues;

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public long getMetricId() {
    return metricId;
  }

  public void setMetricId(long metricId) {
    this.metricId = metricId;
  }

  public String getMetricPath() {
    return metricPath;
  }

  public void setMetricPath(String metricPath) {
    this.metricPath = metricPath;
  }

  public String getFrequency() {
    return frequency;
  }

  public void setFrequency(String frequency) {
    this.frequency = frequency;
  }

  public AppdynamicsMetricDataValue[] getMetricValues() {
    return metricValues;
  }

  public void setMetricValues(AppdynamicsMetricDataValue[] metricValues) {
    this.metricValues = metricValues;
  }

  @Override
  public String toString() {
    return "AppdynamicsMetricData{"
        + "metricName='" + metricName + '\'' + ", metricId=" + metricId + ", metricPath='" + metricPath + '\''
        + ", frequency='" + frequency + '\'' + ", metricValues=" + Arrays.toString(metricValues) + '}';
  }

  public static final class Builder {
    private String metricName;
    private long metricId;
    private String metricPath;
    private String frequency;
    private AppdynamicsMetricDataValue[] metricValues;

    private Builder() {}

    public static Builder anAppdynamicsMetricsData() {
      return new Builder();
    }

    public Builder withMetricName(String metricName) {
      this.metricName = metricName;
      return this;
    }

    public Builder withMetricId(long metricId) {
      this.metricId = metricId;
      return this;
    }

    public Builder withMetricPath(String metricPath) {
      this.metricPath = metricPath;
      return this;
    }

    public Builder withFrequency(String frequency) {
      this.frequency = frequency;
      return this;
    }

    public Builder withMetricValues(AppdynamicsMetricDataValue[] amdv) {
      this.metricValues = amdv;
      return this;
    }

    public Builder but() {
      return anAppdynamicsMetricsData()
          .withMetricName(metricName)
          .withMetricId(metricId)
          .withMetricPath(metricPath)
          .withFrequency(frequency)
          .withMetricValues(metricValues);
    }

    public AppdynamicsMetricData build() {
      AppdynamicsMetricData appdynamicsMetricData = new AppdynamicsMetricData();
      appdynamicsMetricData.setMetricName(metricName);
      appdynamicsMetricData.setMetricId(metricId);
      appdynamicsMetricData.setMetricPath(metricPath);
      appdynamicsMetricData.setFrequency(frequency);
      appdynamicsMetricData.setMetricValues(metricValues);
      return appdynamicsMetricData;
    }
  }
}
