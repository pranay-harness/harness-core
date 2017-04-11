package software.wings.metrics;

import com.github.reinert.jjschema.Attributes;

/**
 * Metrics that reflect the time taken to do something.
 * Created by mike@ on 4/7/17.
 */
public class PercentageMetric<T extends Number> extends Metric<T> {
  @Attributes(required = true, title = "Threshold", description = "3.0") private double threshold;
  // if alertWhenMoreThan is true, the bad state is when value is above threshold
  // conversely, if alertWhenMoreThan is false, the bad state is when value is below threshold
  @Attributes(required = true, title = "Alert When More Than?", description = "True") private boolean alertWhenMoreThan;

  public PercentageMetric(String name, String path, MetricType type, double threshold, boolean alertWhenMoreThan) {
    super(name, path, type);
    this.threshold = threshold;
    this.alertWhenMoreThan = alertWhenMoreThan;
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