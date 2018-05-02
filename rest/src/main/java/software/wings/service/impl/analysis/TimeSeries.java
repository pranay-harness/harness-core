package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import software.wings.metrics.MetricType;

/**
 * Created by rsingh on 3/14/18.
 */
@Data
@Builder
public class TimeSeries {
  private String txnName;
  private String url;
  private String metricName;
  private MetricType metricType;
}
