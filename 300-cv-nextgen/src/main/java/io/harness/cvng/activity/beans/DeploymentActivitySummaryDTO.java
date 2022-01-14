package io.harness.cvng.activity.beans;

import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeploymentActivitySummaryDTO {
  String serviceName;
  String serviceIdentifier;
  String envName;
  String envIdentifier;
  String deploymentTag;
  DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary;
}
