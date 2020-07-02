package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.beans.TimeSeriesMetricType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceGuardTxnMetricAnalysisDataDTO {
  private boolean longTermPattern;
  private long lastSeenTime;
  private int risk;
  private boolean isKeyTransaction;
  private List<Double> shortTermHistory;
  private List<TimeSeriesAnomalousPatterns> anomalousPatterns;
  private TimeSeriesCumulativeSums.MetricSum cumulativeSums;
  private TimeSeriesMetricType metricType;
}
