package software.wings.service.impl.appdynamics;

import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 5/17/17.
 */
@Data
@Builder
public class AppdynamicsMetricData {
  private String metricName;
  private long metricId;
  private String metricPath;
  private String frequency;
  private AppdynamicsMetricDataValue[] metricValues;
}
