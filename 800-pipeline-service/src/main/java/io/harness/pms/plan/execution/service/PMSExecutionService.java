package io.harness.pms.plan.execution.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.plan.execution.PlanExecutionInterruptType;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.InterruptDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionFilterPropertiesDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public interface PMSExecutionService {
  String getInputSetYaml(
      String accountId, String orgId, String projectId, String planExecutionId, boolean pipelineDeleted);

  Page<PipelineExecutionSummaryEntity> getPipelineExecutionSummaryEntity(Criteria criteria, Pageable pageable);

  PipelineExecutionSummaryEntity getPipelineExecutionSummaryEntity(
      String accountId, String orgId, String projectId, String planExecutionId, boolean pipelineDeleted);

  OrchestrationGraphDTO getOrchestrationGraph(String stageNodeId, String planExecutionId);

  InterruptDTO registerInterrupt(
      PlanExecutionInterruptType executionInterruptType, String planExecutionId, String nodeExecutionId);

  Criteria formCriteria(String accountId, String orgId, String projectId, String pipelineIdentifier,
      String filterIdentifier, PipelineExecutionFilterPropertiesDTO filterProperties, String moduleName,
      String searchTerm, ExecutionStatus status, boolean myDeployments, boolean pipelineDeleted);

  void deleteExecutionsOnPipelineDeletion(PipelineEntity pipelineEntity);
}
