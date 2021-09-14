/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.beans;

import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.beans.TimeSeriesMetricType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceGuardTxnMetricAnalysisDataDTO {
  private boolean longTermPattern;
  private long lastSeenTime;
  private int risk;
  private double score;
  private boolean isKeyTransaction;
  private List<Double> shortTermHistory;
  private List<TimeSeriesAnomalies> anomalousPatterns;
  private TimeSeriesCumulativeSums.MetricSum cumulativeSums;
  private TimeSeriesMetricType metricType;
  public Risk getRisk() {
    return Risk.valueOf(risk);
  }
}
