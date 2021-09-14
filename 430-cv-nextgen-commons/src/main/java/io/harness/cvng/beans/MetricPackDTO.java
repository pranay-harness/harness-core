/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricPackDTO {
  String uuid;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  DataSourceType dataSourceType;
  String identifier;
  CVMonitoringCategory category;
  Set<MetricDefinitionDTO> metrics;
  List<TimeSeriesThresholdDTO> thresholds;

  @Value
  @Builder
  public static class MetricDefinitionDTO {
    String name;
    TimeSeriesMetricType type;
    String path;
    String validationPath;
    String responseJsonPath;
    String validationResponseJsonPath;
    List<TimeSeriesThresholdDTO> thresholds;
    boolean included;
  }
}
