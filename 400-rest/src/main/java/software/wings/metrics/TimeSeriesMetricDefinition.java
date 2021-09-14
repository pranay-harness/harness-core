/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
