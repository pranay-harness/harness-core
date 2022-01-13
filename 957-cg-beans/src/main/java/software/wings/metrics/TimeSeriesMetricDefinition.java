/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 11/29/17.
 */
@Data
@Builder
public class TimeSeriesMetricDefinition {
  private String metricName;
  private MetricType metricType;
  private Set<String> tags;
  private List<Threshold> customThresholds;
  private Map<ThresholdCategory, List<Threshold>> categorizedThresholds;

  @JsonProperty("metricType")
  public String getMetricTypeName() {
    return metricType.name();
  }

  @JsonProperty("thresholds")
  public List<Threshold> getThresholds() {
    return metricType.getThresholds();
  }
}
