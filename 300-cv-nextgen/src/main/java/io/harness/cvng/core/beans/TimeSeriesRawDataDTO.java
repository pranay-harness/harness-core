/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
public class TimeSeriesRawDataDTO {
  private String cvConfigId;
  private Map<String, Map<String, List<MetricData>>> transactionMetricValues;

  @Data
  @Builder
  public static class MetricData {
    private long timestamp;
    private double value;
  }
}
