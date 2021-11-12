package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthSourceMetricDefinition {
  RiskProfile riskProfile;
  AnalysisDTO analysis;
  SLIDTO sli;

  public RiskProfile getRiskProfile() {
    if (riskProfile == null) {
      return analysis.riskProfile;
    }
    return riskProfile;
  }

  @Data
  @Builder
  public static class AnalysisDTO {
    LiveMonitoringDTO liveMonitoring;
    DeploymentVerificationDTO deploymentVerification;
    RiskProfile riskProfile;

    @Data
    @Builder
    public static class LiveMonitoringDTO {
      Boolean enabled;
    }

    @Data
    @Builder
    public static class DeploymentVerificationDTO {
      Boolean enabled;
      String serviceInstanceFieldName;
    }
  }

  @Data
  @Builder
  public static class SLIDTO {
    Boolean enabled;
  }
}
