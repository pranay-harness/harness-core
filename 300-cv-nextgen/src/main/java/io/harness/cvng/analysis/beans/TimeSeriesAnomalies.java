package io.harness.cvng.analysis.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesAnomalies {
  private String transactionName;
  private String metricName;
  private List<Double> testData;
  private List<Long> anomalousTimestamps;
}
