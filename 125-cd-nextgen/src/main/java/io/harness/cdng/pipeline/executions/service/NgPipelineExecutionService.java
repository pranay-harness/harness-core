package io.harness.cdng.pipeline.executions.service;

import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionInterruptType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionInterruptDTO;
import io.harness.pms.execution.ExecutionStatus;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NgPipelineExecutionService {
  Page<PipelineExecutionSummary> getExecutions(String accountId, String orgId, String projectId, Pageable pageable,
      PipelineExecutionSummaryFilter pipelineExecutionSummaryFilter);

  PipelineExecutionSummary createPipelineExecutionSummary(String accountId, String orgId, String projectId,
      PlanExecution planExecution, CDPipelineSetupParameters cdPipelineSetupParameters);

  PipelineExecutionSummary getByPlanExecutionId(
      String accountId, String orgId, String projectId, String planExecutionId);

  void updateStatusForGivenNode(
      String accountId, String orgId, String projectId, String planExecutionId, NodeExecution nodeExecution);

  PipelineExecutionSummary addServiceInformationToPipelineExecutionNode(String accountId, String orgId,
      String projectId, String planExecutionId, String nodeExecutionId, ServiceOutcome serviceOutcome);

  PipelineExecutionSummary addEnvironmentInformationToPipelineExecutionNode(String accountId, String orgId,
      String projectId, String planExecutionId, String nodeExecutionId, EnvironmentOutcome environmentOutcome);

  List<ExecutionStatus> getExecutionStatuses();

  PipelineExecutionInterruptDTO registerInterrupt(
      PipelineExecutionInterruptType executionInterruptType, String planExecutionId);

  Map<ExecutionNodeType, String> getStepTypeToYamlTypeMapping();
}
