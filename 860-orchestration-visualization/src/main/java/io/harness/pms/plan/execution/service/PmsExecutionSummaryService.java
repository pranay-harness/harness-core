package io.harness.pms.plan.execution.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import java.util.Optional;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PmsExecutionSummaryService {
  void regenerateStageLayoutGraph(String planExecutionId);
  void updateEndTs(String planExecutionId, NodeExecution nodeExecution);
  void update(String planExecutionId, NodeExecution nodeExecution);
  void update(String planExecutionId, Update update);

  Optional<PipelineExecutionSummaryEntity> getPipelineExecutionSummary(
      String accountId, String orgId, String projectId, String planExecutionId);
}
