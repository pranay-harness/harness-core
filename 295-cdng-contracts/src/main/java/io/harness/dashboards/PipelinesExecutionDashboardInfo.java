package io.harness.dashboards;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelinesExecutionDashboardInfo {
  private List<PipelineExecutionDashboardInfo> runningExecutions;
  private List<PipelineExecutionDashboardInfo> pendingApprovalExecutions;
  private List<PipelineExecutionDashboardInfo> pendingManualInterventionExecutions;
  private List<PipelineExecutionDashboardInfo> failed24HrsExecutions;
}
