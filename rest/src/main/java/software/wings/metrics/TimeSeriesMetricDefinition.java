package software.wings.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sriram_parthasarathy on 11/29/17.
 */
@Builder
@Data
public class TimeSeriesMetricDefinition {
  private String metricName;
  private List<Threshold> thresholds = new ArrayList<>();
  private MetricType metricType;
}
