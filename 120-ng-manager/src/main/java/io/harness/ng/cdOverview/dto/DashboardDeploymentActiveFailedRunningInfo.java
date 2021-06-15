package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class DashboardDeploymentActiveFailedRunningInfo {
  private List<DeploymentStatusInfo> failure;
  private List<DeploymentStatusInfo> pending;
  private List<DeploymentStatusInfo> active;
}
