package software.wings.service.impl.appdynamics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by rsingh on 4/19/17.
 */
@Data
@Builder
public class AppdynamicsMetric {
  private AppdynamicsMetricType type;
  private String name;
  private List<AppdynamicsMetric> childMetrices;

  public enum AppdynamicsMetricType { leaf, folder }
}
