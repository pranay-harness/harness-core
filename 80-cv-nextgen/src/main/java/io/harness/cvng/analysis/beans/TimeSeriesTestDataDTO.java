package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TimeSeriesTestDataDTO {
  private String cvConfigId;
  private Map<String, Map<String, List<Double>>> transactionMetricValues;
}
