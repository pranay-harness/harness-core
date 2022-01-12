/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

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
