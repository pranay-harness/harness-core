package io.harness.cvng.core.beans;

import io.harness.beans.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthSourceMetricDefinition implements WithIdentifier {
  // @EntityIdentifier // TODO: uncomment and enable the test once UI starts sending this.
  String identifier;
  @NotNull private String metricName;
  RiskProfile riskProfile;
  AnalysisDTO analysis;
  SLIDTO sli;

  public String getIdentifier() {
    if (this.identifier == null) { // TODO: remove. using only till migration and UI change
      return metricName.replaceAll("[^A-Za-z0-9]", "");
    }
    return this.identifier;
  }

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
      // TODO: Make this HealthSource type specific
      String serviceInstanceFieldName;
      String serviceInstanceMetricPath;
    }
  }

  @Data
  @Builder
  public static class SLIDTO {
    Boolean enabled;
  }
}
