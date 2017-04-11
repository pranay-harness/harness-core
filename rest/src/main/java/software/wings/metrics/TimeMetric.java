package software.wings.metrics;

import com.github.reinert.jjschema.Attributes;

import java.util.concurrent.TimeUnit;

/**
 * Metrics that reflect the time taken to do something.
 * Created by mike@ on 4/7/17.
 */
public class TimeMetric extends Metric {
  @Attributes(required = true, title = "Unit", description = "MILLISECONDS") private TimeUnit timeUnit;
  @Attributes(required = true, title = "Threshold", description = "3.0") private double threshold;
  // if alertWhenMoreThan is true, the bad state is when value is above threshold
  // conversely, if alertWhenMoreThan is false, the bad state is when value is below threshold
  @Attributes(required = true, title = "Alert When More Than?", description = "True") private boolean alertWhenMoreThan;

  public TimeMetric(
      String name, String path, MetricType type, TimeUnit timeUnit, double threshold, boolean alertWhenMoreThan) {
    super(name, path, type);
    this.timeUnit = timeUnit;
    this.threshold = threshold;
    this.alertWhenMoreThan = alertWhenMoreThan;
  }

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public void setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public double getThreshold() {
    return threshold;
  }

  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  public boolean isAlertWhenMoreThan() {
    return alertWhenMoreThan;
  }

  public void setAlertWhenMoreThan(boolean alertWhenMoreThan) {
    this.alertWhenMoreThan = alertWhenMoreThan;
  }
}
