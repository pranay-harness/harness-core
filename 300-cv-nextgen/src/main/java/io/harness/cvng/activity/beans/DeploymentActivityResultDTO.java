package io.harness.cvng.activity.beans;

import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.verificationjob.beans.AdditionalInfo;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
public class DeploymentActivityResultDTO {
  String deploymentTag;
  String serviceName;
  String serviceIdentifier;
  Set<String> environments;
  DeploymentResultSummary deploymentResultSummary;

  @Data
  @Builder
  public static class DeploymentVerificationJobInstanceSummary {
    int progressPercentage;
    Long startTime;
    Long durationMs;
    Double riskScore;
    String environmentName;
    String jobName;
    String verificationJobInstanceId;
    String activityId;
    long activityStartTime;
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
