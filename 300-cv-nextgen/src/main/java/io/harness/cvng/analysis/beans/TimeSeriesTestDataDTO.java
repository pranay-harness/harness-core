/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.beans;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeSeriesTestDataDTO {
  private String cvConfigId;
  private Map<String, Map<String, List<Double>>> transactionMetricValues;

  private Map<String, Map<String, List<MetricData>>> metricGroupValues;

  @Data
  @Builder
  public static class MetricData {
    private long timestamp;
    private double value;
    private int risk;
    public Risk getRisk() {
      return Risk.valueOf(risk);
    }
  }
}
