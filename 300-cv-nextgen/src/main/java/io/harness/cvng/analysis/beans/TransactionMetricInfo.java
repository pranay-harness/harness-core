package io.harness.cvng.analysis.beans;

import java.util.SortedSet;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Data
@Builder
public class TransactionMetricInfo {
  private TransactionMetric transactionMetric;
  private String connectorName;
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
