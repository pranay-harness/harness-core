package io.harness.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
public class DeploymentStatsSummary {
  long totalCount;
  @Builder.Default double totalCountChangeRate = DashboardHelper.MAX_VALUE;
  @Builder.Default double failureRate = DashboardHelper.MAX_VALUE;
  @Builder.Default double failureRateChangeRate = DashboardHelper.MAX_VALUE;
  @Builder.Default double deploymentRate = DashboardHelper.MAX_VALUE;
  @Builder.Default double deploymentRateChangeRate = DashboardHelper.MAX_VALUE;

  List<TimeBasedDeploymentInfo> timeBasedDeploymentInfoList;
}
