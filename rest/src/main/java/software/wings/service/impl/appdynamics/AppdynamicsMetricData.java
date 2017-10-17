package software.wings.service.impl.appdynamics;

import lombok.Data;

/**
 * Created by rsingh on 5/17/17.
 */
@Data
public class AppdynamicsMetricData {
  private String metricName;
  private long metricId;
  private String metricPath;
  private String frequency;
  private AppdynamicsMetricDataValue[] metricValues;
}
