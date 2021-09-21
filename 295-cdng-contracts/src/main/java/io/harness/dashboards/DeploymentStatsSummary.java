package io.harness.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class DeploymentStatsSummary {
  long count;
  double countChangeRate;
  long failureCount;
  double failureChangeRate;
  double failureRate;
  double failureRateChangeRate;
  double deploymentRate;
  double deploymentRateChangeRate;
  long runningCount;
  long pendingApprovalsCount;
  long manualInterventionsCount;
  long failed24HoursCount;
}
