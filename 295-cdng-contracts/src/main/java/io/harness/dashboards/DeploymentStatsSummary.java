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
  double totalCountChangeRate;
  double failureRate;
  double failureRateChangeRate;
  double deploymentRate;
  double deploymentRateChangeRate;

  List<TimeBasedDeploymentInfo> timeBasedDeploymentInfoList;
}
