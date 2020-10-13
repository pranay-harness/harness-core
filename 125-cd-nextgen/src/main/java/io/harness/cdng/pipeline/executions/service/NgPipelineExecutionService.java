package io.harness.cdng.pipeline.executions.service;

import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionInterruptType;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.cdng.pipeline.executions.beans.dto.PipelineExecutionInterruptDTO;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.executions.steps.ExecutionNodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public interface NgPipelineExecutionService {
  Page<PipelineExecutionSummary> getExecutions(String accountId, String orgId, String projectId, Pageable pageable,
      PipelineExecutionSummaryFilter pipelineExecutionSummaryFilter);

  PipelineExecutionSummary createPipelineExecutionSummary(
      String accountId, String orgId, String projectId, PlanExecution planExecution, NgPipeline ngPipeline);

  PipelineExecutionDetail getPipelineExecutionDetail(@Nonnull String planExecutionId, String stageId);

  PipelineExecutionSummary getByPlanExecutionId(
      String accountId, String orgId, String projectId, String planExecutionId);

  PipelineExecutionSummary updateStatusForGivenNode(
      String accountId, String orgId, String projectId, String planExecutionId, NodeExecution nodeExecution);

  PipelineExecutionSummary addServiceInformationToPipelineExecutionNode(String accountId, String orgId,
      String projectId, String planExecutionId, String nodeExecutionId, ServiceOutcome serviceOutcome);

  List<ExecutionStatus> getExecutionStatuses();

  PipelineExecutionInterruptDTO registerInterrupt(
      PipelineExecutionInterruptType executionInterruptType, String planExecutionId);

  Map<ExecutionNodeType, String> getStepTypeToYamlTypeMapping();
}
