package io.harness.cvng.dashboard.beans;

import io.harness.cvng.beans.CVMonitoringCategory;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
@Value
@Builder
public class RiskSummaryPopoverDTO {
  CVMonitoringCategory category;
  @Singular("addEnvSummary") List<EnvSummary> envSummaries;
  @Value
  @Builder
  public static class EnvSummary {
    int riskScore;
    String envName;
    String envIdentifier;
    @Singular("addServiceSummary") List<ServiceSummary> serviceSummaries;
  }
  @Value
  @Builder
  public static class ServiceSummary {
    String serviceName;
    String serviceIdentifier;
    Integer risk;
    List<AnalysisRisk> analysisRisks;
  }
  @Value
  @Builder
  public static class AnalysisRisk {
    String name;
    Integer risk;
  }
}
