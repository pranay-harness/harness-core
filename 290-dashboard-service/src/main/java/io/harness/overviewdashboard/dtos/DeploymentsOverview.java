package io.harness.overviewdashboard.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.PipelineExecutionDashboardInfo;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class DeploymentsOverview {
  List<PipelineExecutionDashboardInfo> runningExecutions;
  List<PipelineExecutionDashboardInfo> pendingApprovalExecutions;
  List<PipelineExecutionDashboardInfo> pendingManualInterventionExecutions;
  List<PipelineExecutionDashboardInfo> failed24HrsExecutions;
}
