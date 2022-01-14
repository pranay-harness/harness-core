package io.harness.cvng.analysis.beans;

import io.harness.cvng.beans.DataSourceType;

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
  private DataSourceType dataSourceType;
  private SortedSet<DeploymentTimeSeriesAnalysisDTO.HostData> nodes;

  @Value
  @Builder
  @EqualsAndHashCode(of = {"transactionName", "metricName"})
  public static class TransactionMetric {
    String transactionName;
    String metricName;
    private Double score; // is this score is needed in the UI?
    private Risk risk;
  }
}
