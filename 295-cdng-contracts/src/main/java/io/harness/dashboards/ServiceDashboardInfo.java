package io.harness.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
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
  @Builder.Default double totalDeploymentsChangeRate = DashboardHelper.MAX_VALUE;

  long instancesCount;
  @Builder.Default double instancesCountChangeRate = DashboardHelper.MAX_VALUE;
}
