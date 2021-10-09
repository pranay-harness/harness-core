package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeploymentTimeSeriesAnalysisDTO {
  @JsonDeserialize(using = RiskDeserializer.class) int risk;
  public Risk getRisk() {
    return Risk.valueOf(risk);
  }
  Double score;
  List<HostInfo> hostSummaries;
  List<TransactionMetricHostData> transactionMetricSummaries;

  public List<TransactionMetricHostData> getTransactionMetricSummaries() {
    if (this.transactionMetricSummaries == null) {
      return Collections.emptyList();
    }
    return transactionMetricSummaries;
  }

  public List<HostInfo> getHostSummaries() {
    if (hostSummaries == null) {
      return Collections.emptyList();
    }
    return hostSummaries;
  }

  @Value
  @Builder
  public static class HostInfo {
    String hostName;
    boolean primary;
    boolean canary;
    @JsonDeserialize(using = RiskDeserializer.class) int risk;
    public Risk getRisk() {
      return Risk.valueOf(risk);
    }
    Double score;
  }

  @Value
  @Builder
  public static class HostData implements Comparable<HostData> {
    String hostName;
    @JsonDeserialize(using = RiskDeserializer.class) int risk;
    public Risk getRisk() {
      return Risk.valueOf(risk);
    }
    Double score;
    List<Double> controlData;
    List<Double> testData;
    public boolean isAnomalous() {
      return getRisk().getValue() >= Risk.MEDIUM.getValue();
    }
    public Optional<String> getHostName() {
      return Optional.ofNullable(hostName);
    }

    @Override
    public int compareTo(@NotNull HostData o) {
      int result = Double.compare(o.getScore(), this.getScore());
      return result == 0 && o.getHostName().isPresent() && this.getHostName().isPresent()
          ? o.getHostName().get().compareTo(this.getHostName().get())
          : result;
    }
  }

  @Value
  @Builder
  public static class TransactionMetricHostData {
    String transactionName;
    String metricName;
    @JsonDeserialize(using = RiskDeserializer.class) int risk;
    public Risk getRisk() {
      return Risk.valueOf(this.risk);
    }

    Double score;
    // TODO: For load test, this is overall data. Figure out a better name that suits for both canary and load test
    List<HostData> hostData;

    public List<HostData> getHostData() {
      if (hostData == null) {
        return Collections.emptyList();
      }
      return hostData;
    }

    public boolean isAnomalous() {
      return getRisk().getValue() >= Risk.MEDIUM.getValue();
    }
  }
}
