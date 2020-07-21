package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;
import java.util.Map;

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
