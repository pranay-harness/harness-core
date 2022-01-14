package io.harness.dashboards;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelinesExecutionDashboardInfo {
  @Builder.Default private List<PipelineExecutionDashboardInfo> runningExecutions = new ArrayList<>();
  @Builder.Default private List<PipelineExecutionDashboardInfo> pendingApprovalExecutions = new ArrayList<>();
  @Builder.Default private List<PipelineExecutionDashboardInfo> pendingManualInterventionExecutions = new ArrayList<>();
  @Builder.Default private List<PipelineExecutionDashboardInfo> failed24HrsExecutions = new ArrayList<>();
}
