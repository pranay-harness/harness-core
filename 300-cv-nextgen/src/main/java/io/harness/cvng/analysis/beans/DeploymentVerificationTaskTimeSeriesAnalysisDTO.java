package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DeploymentVerificationTaskTimeSeriesAnalysisDTO {
  ResultSummary resultSummary;
  List<HostSummary> hostSummaries;
  @Value
  @Builder
  public static class ResultSummary {
    int risk;
    Double score;
    List<TransactionSummary> transactionSummaries;
  }
  @Value
  @Builder
  public static class HostSummary {
    String hostName;
    String isNewHost;
    ResultSummary resultSummary;
  }
  @Value
  @Builder
  public static class TransactionSummary {
    String transactionName;
    List<MetricSummary> metricSummaries;
  }
  @Value
  @Builder
  public static class MetricSummary {
    String metricName;
    int risk; // -1 means n/a no analysis done.
    Double score; // greater then 0. No higher boundary.. Mean of 1. Won't be present if risk is -1.
    List<Double> controlData;
    List<Double> testData;
  }
}
