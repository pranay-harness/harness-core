package io.harness.cdng.pipeline.executions.service;

import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionInterruptType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionInterruptDTO;
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
