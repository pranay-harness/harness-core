package io.harness.cvng.activity.beans;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class DeploymentActivityVerificationResultDTO {
  String tag;
  String serviceName;
  String serviceIdentifier;
  ActivityVerificationSummary preProductionDeploymentSummary;
  ActivityVerificationSummary productionDeploymentSummary;
  ActivityVerificationSummary postDeploymentSummary;
}
