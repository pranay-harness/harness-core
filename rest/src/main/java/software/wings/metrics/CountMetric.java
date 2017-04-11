package software.wings.metrics;

import com.github.reinert.jjschema.Attributes;

/**
 * Metrics that reflect the time taken to do something.
 * Created by mike@ on 4/10/17.
 */
public class CountMetric<T extends Number> extends Metric<T> {
  @Attributes(required = true, title = "Threshold", description = "3") private int threshold;
  // if alertWhenMoreThan is true, the bad state is when value is above threshold
  // conversely, if alertWhenMoreThan is false, the bad state is when value is below threshold
  @Attributes(required = true, title = "Alert When More Than?", description = "True") private boolean alertWhenMoreThan;

  public CountMetric(String name, String path, MetricType type, int threshold, boolean alertWhenMoreThan) {
    super(name, path, type);
    this.threshold = threshold;
    this.alertWhenMoreThan = alertWhenMoreThan;
  }

  public int getThreshold() {
    return threshold;
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  public boolean isAlertWhenMoreThan() {
    return alertWhenMoreThan;
  }

  public void setAlertWhenMoreThan(boolean alertWhenMoreThan) {
    this.alertWhenMoreThan = alertWhenMoreThan;
  }
}