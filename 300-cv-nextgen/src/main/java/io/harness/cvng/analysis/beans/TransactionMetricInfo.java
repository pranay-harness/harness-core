package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.SortedSet;

@Data
@Builder
public class TransactionMetricInfo {
  private TransactionMetric transactionMetric;
  private SortedSet<DeploymentTimeSeriesAnalysisDTO.HostData> nodes;

  @Value
  @Builder
  @EqualsAndHashCode(of = {"transactionName", "metricName"})
  public static class TransactionMetric {
    String transactionName;
    String metricName;
    private Double score;
  }
}
