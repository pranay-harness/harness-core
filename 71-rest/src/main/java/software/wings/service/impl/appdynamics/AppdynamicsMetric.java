package software.wings.service.impl.appdynamics;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rsingh on 4/19/17.
 */
@Data
@Builder
public class AppdynamicsMetric {
  private AppdynamicsMetricType type;
  private String name;
  @Default private List<AppdynamicsMetric> childMetrices = new ArrayList<>();

  public enum AppdynamicsMetricType { leaf, folder }
}
