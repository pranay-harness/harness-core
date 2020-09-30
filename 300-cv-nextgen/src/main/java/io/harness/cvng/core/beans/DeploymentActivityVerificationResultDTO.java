package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
@Data
@Builder
public class DeploymentActivityVerificationResultDTO {
  String tag;
  String serviceName;
  DeploymentSummary preProductionDeploymentSummary;
  DeploymentSummary productionDeploymentSummary;
  DeploymentSummary postDeploymentSummary;
  @Value
  @Builder
  public static class DeploymentSummary {
    int total;
    int passed;
    int failed;
    int errors;
    int progress;
    int notStarted;
    long timeRemainingMs;
    int progressPercentage;
    Long startTime;
    Long durationMs;
    Double riskScore;
  }
}
