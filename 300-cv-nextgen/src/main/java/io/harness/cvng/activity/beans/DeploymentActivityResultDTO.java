package io.harness.cvng.activity.beans;

import io.harness.cvng.verificationjob.beans.AdditionalInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class DeploymentActivityResultDTO {
  String deploymentTag;
  String serviceName;
  String serviceIdentifier;
  Set<String> environments;
  DeploymentResultSummary deploymentResultSummary;

  @Value
  @Builder
  public static class DeploymentVerificationJobInstanceSummary {
    int progressPercentage;
    Long startTime;
    Long durationMs;
    Double riskScore;
    String environmentName;
    String jobName;
    String verificationJobInstanceId;
    ActivityVerificationStatus status;
    AdditionalInfo additionalInfo;
  }

  @Value
  @Builder
  public static class DeploymentResultSummary {
    List<DeploymentVerificationJobInstanceSummary> preProductionDeploymentVerificationJobInstanceSummaries;
    List<DeploymentVerificationJobInstanceSummary> productionDeploymentVerificationJobInstanceSummaries;
    List<DeploymentVerificationJobInstanceSummary> postDeploymentVerificationJobInstanceSummaries;
  }
}
