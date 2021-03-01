package io.harness.cvng.activity.beans;

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
  DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary;
}
