package software.wings.service.impl.cloudwatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 3/30/18.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudWatchMetric {
  private String metricName;
  private String displayName;
  private String dimension;
  private String dimensionDisplay;
  private String metricType;
  private boolean enabledDefault;
}
