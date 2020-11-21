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
  }
}
