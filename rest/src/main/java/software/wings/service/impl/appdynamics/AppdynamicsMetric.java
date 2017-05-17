package software.wings.service.impl.appdynamics;

import java.util.List;

/**
 * Created by rsingh on 4/19/17.
 */
public class AppdynamicsMetric {
  private AppdynamicsMetricType type;
  private String name;
  private List<AppdynamicsMetric> childMetrices;

  public AppdynamicsMetricType getType() {
    return type;
  }

  public void setType(AppdynamicsMetricType type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<AppdynamicsMetric> getChildMetrices() {
    return childMetrices;
  }

  public void setChildMetrices(List<AppdynamicsMetric> childMetrices) {
    this.childMetrices = childMetrices;
  }

  public enum AppdynamicsMetricType { leaf, folder }

  @Override
  public String toString() {
    return "AppdynamicsMetric{"
        + "type=" + type + ", name='" + name + '\'' + ", childMetrices=" + childMetrices + '}';
  }
}
