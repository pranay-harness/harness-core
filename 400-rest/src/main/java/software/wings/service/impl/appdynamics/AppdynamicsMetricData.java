package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.HasPredicate.hasNone;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 5/17/17.
 */
@Data
@Builder
public class AppdynamicsMetricData implements Comparable<AppdynamicsMetricData> {
  private String metricName;
  private long metricId;
  private String metricPath;
  private String frequency;
  private List<AppdynamicsMetricDataValue> metricValues;

  @Override
  public int compareTo(AppdynamicsMetricData o) {
    if (hasNone(metricValues) && hasNone(o.metricValues)) {
      return metricName.compareTo(o.metricName);
    }

    if (!hasNone(metricValues) && hasNone(o.metricValues)) {
      return -1;
    }

    if (hasNone(metricValues) && !hasNone(o.metricValues)) {
      return 1;
    }

    return metricValues.size() - o.metricValues.size();
  }
}
