package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.infrastructuredetails.InfrastructureDetails;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class InstanceDetailsDTO {
  String podName;
  String artifactName;
  String connectorRef;
  InfrastructureDetails infrastructureDetails;
  String terraformInstance;
  long deployedAt;
  String deployedById;
  String deployedByName;
  String pipelineExecutionName;
}
