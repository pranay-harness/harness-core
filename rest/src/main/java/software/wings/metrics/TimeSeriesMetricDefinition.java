package software.wings.service.impl.newrelic;

import lombok.Builder;
import software.wings.metrics.MetricType;
import software.wings.metrics.Threshold;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sriram_parthasarathy on 11/29/17.
 */
@Builder
public class TimeSeriesMetricDefinition {
  private List<Threshold> thresholds = new ArrayList<>();
  private double minThreshold;
  private MetricType metricType;
}
