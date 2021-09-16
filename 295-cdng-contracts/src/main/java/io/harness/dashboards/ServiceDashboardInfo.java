package io.harness.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class ServiceDashboardInfo {
  String name;
  String identifier;
  String orgIdentifier;
  String projectIdentifier;
  String accountIdentifier;

  long totalDeploymentsCount;
  long successDeploymentsCount;
  long failureDeploymentsCount;
  double totalDeploymentsChangeRate;

  long instancesCount;
  double instancesCountChangeRate;
}
