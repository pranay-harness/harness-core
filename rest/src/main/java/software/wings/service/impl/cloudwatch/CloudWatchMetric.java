package software.wings.service.impl.cloudwatch;

import lombok.Builder;
import lombok.Data;
import software.wings.metrics.MetricType;

/**
 * Created by rsingh on 3/30/18.
 */
@Data
@Builder
public class CloudWatchMetric {
  private String metricName;
  private String displayName;
  private String dimension;
  private String dimensionDisplay;
  private MetricType metricType;
  private boolean enabledDefault;
}
