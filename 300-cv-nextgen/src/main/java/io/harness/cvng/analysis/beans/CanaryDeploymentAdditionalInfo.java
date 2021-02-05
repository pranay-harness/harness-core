package io.harness.cvng.analysis.beans;

import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.verificationjob.beans.AdditionalInfo;

import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Data
@Builder
public class CanaryDeploymentAdditionalInfo extends AdditionalInfo {
  Set<HostSummaryInfo> primary;
  Set<HostSummaryInfo> canary;

  private String primaryInstancesLabel;
  private String canaryInstancesLabel;

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
    Risk risk;
    long anomalousMetricsCount;
    long anomalousLogClustersCount;
  }

  @Value
  @Builder
  public static class TrafficSplitPercentage {
    double preDeploymentPercentage;
    double postDeploymentPercentage;
  }

  public enum CanaryAnalysisType {
    CLASSIC("primary", "canary"),
    IMPROVISED("before", "after");

    private final String primaryInstancesLabel;
    private final String canaryInstancesLabel;

    CanaryAnalysisType(String primaryInstancesLabel, String canaryInstancesLabel) {
      this.primaryInstancesLabel = primaryInstancesLabel;
      this.canaryInstancesLabel = canaryInstancesLabel;
    }

    public String getCanaryInstancesLabel() {
      return this.canaryInstancesLabel;
    }

    public String getPrimaryInstancesLabel() {
      return this.primaryInstancesLabel;
    }
  }

  public void setFieldNames() {
    Preconditions.checkNotNull(this.primary, "Populate control hosts before setting field names");
    Preconditions.checkNotNull(this.canary, "Populate test hosts before setting field names");

    Set<String> controlHosts = this.getPrimary().stream().map(HostSummaryInfo::getHostName).collect(Collectors.toSet());
    Set<String> testHosts = this.getCanary().stream().map(HostSummaryInfo::getHostName).collect(Collectors.toSet());
    testHosts.removeAll(controlHosts);

    CanaryAnalysisType canaryAnalysisType =
        testHosts.size() > 0 ? CanaryAnalysisType.CLASSIC : CanaryAnalysisType.IMPROVISED;
    this.setPrimaryInstancesLabel(canaryAnalysisType.getPrimaryInstancesLabel());
    this.setCanaryInstancesLabel(canaryAnalysisType.getCanaryInstancesLabel());
  }
}
