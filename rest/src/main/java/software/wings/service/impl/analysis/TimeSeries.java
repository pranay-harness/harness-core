package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.metrics.MetricType;

/**
 * Created by rsingh on 3/14/18.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeries {
  private String txnName;
  private String url;
  private String metricName;
  private MetricType metricType;
}
