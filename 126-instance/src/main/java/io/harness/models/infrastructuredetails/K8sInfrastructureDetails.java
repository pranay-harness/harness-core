package io.harness.models.infrastructuredetails;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class K8sInfrastructureDetails extends InfrastructureDetails {
  String namespace;
  String releaseName;
}
