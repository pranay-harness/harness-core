package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class DeploymentTimeSeriesAnalysisDTO {
  int risk;
  Double score;
  List<HostInfo> hostSummaries;
  List<TransactionMetricHostData> transactionMetricSummaries;

  @Value
  @Builder
  public static class HostInfo {
    String hostName;
    boolean presentBeforeDeployment;
    boolean presentAfterDeployment;
    int risk;
    Double score;
  }

  @Value
  @Builder
  public static class HostData implements Comparable<HostData> {
    String hostName;
    int risk;
    Double score;
    List<Double> controlData;
    List<Double> testData;

    @Override
    public int compareTo(@NotNull HostData o) {
      int result = Double.compare(o.getScore(), this.getScore());
      return result == 0 ? o.getHostName().compareTo(this.getHostName()) : result;
    }
  }

  @Value
  @Builder
  public static class TransactionMetricHostData {
    String transactionName;
    String metricName;
    int risk;
    Double score;
    List<HostData> hostData;
  }
}
