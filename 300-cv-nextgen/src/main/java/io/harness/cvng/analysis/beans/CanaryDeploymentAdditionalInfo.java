package io.harness.cvng.analysis.beans;

import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO.TimeSeriesRisk;
import io.harness.cvng.verificationjob.beans.AdditionalInfo;
import io.harness.cvng.verificationjob.beans.VerificationJobType;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Data
@Builder
public class CanaryDeploymentAdditionalInfo extends AdditionalInfo {
  Set<HostSummaryInfo> primary;
  Set<HostSummaryInfo> canary;
  TrafficSplitPercentage trafficSplitPercentage;

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.CANARY;
  }

  @Data
  @Builder
  @EqualsAndHashCode(of = "hostName")
  public static class HostSummaryInfo {
    String hostName;
    TimeSeriesRisk riskScore;
    long anomalousMetricsCount;
    long anomalousLogClustersCount;
  }

  @Value
  @Builder
  public static class TrafficSplitPercentage {
    double preDeploymentPercentage;
    double postDeploymentPercentage;
  }
}
