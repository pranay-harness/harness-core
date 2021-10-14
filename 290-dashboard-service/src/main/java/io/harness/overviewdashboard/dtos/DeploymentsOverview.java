package io.harness.overviewdashboard.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class DeploymentsOverview {
  List<PipelineExecutionInfo> runningExecutions;
  List<PipelineExecutionInfo> pendingApprovalExecutions;
  List<PipelineExecutionInfo> pendingManualInterventionExecutions;
  List<PipelineExecutionInfo> failed24HrsExecutions;
}
