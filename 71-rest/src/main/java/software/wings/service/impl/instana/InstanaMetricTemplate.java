package software.wings.service.impl.instana;

import lombok.Data;
import software.wings.metrics.MetricType;
@Data
public class InstanaMetricTemplate {
  private String metricName;
  private String displayName;
  private MetricType metricType;
  private String aggregation;
}
