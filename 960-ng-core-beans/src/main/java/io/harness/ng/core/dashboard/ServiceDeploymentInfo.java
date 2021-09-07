package io.harness.ng.core.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class ServiceDeploymentInfo {
  private String serviceName;
  private String serviceTag;
}
