package software.wings.service.impl.instana;

import software.wings.metrics.MetricType;

import lombok.Data;
@Data
public class InstanaMetricTemplate {
  private String metricName;
  private String displayName;
  private MetricType metricType;
  private String aggregation;
}
