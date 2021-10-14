package io.harness.pms.plan.execution.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PmsExecutionSummaryService {
  void regenerateStageLayoutGraph(String planExecutionId);
  void updateEndTs(String planExecutionId, NodeExecution nodeExecution);
  void update(String planExecutionId, NodeExecution nodeExecution);
}
