package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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
    if (isEmpty(metricValues) && isEmpty(o.metricValues)) {
      return metricName.compareTo(o.metricName);
    }

    if (!isEmpty(metricValues) && isEmpty(o.metricValues)) {
      return -1;
    }

    if (isEmpty(metricValues) && !isEmpty(o.metricValues)) {
      return 1;
    }

    return metricValues.size() - o.metricValues.size();
  }
}
