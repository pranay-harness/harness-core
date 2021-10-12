package io.harness.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
public class ProjectDashBoardInfo {
  String projectIdentifier;
  String orgIdentifier;
  String accountId;

  long deploymentsCount;
  double deploymentsCountChangeRate;
  long successDeploymentsCount;
  long failedDeploymentsCount;
}
