package io.harness.cdng.pipeline.executions.service;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;

public interface NgPipelineExecutionService {
  PlanExecution triggerPipeline(
      String pipelineYaml, String accountId, String orgId, String projectId, EmbeddedUser user);

  Page<PipelineExecutionSummary> getExecutions(String accountId, String orgId, String projectId, Pageable pageable);

  PipelineExecutionSummary createPipelineExecutionSummary(
      String accountId, String orgId, String projectId, PlanExecution planExecution, NgPipeline ngPipeline);

  PipelineExecutionDetail getPipelineExecutionDetail(@Nonnull String planExecutionId, String stageId);

  PipelineExecutionSummary getByPlanExecutionId(
      String accountId, String orgId, String projectId, String planExecutionId);

  PipelineExecutionSummary updateStatusForGivenNode(
      String accountId, String orgId, String projectId, String planExecutionId, NodeExecution nodeExecution);
}
