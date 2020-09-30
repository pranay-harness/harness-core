package io.harness.cdng.pipeline.executions.service;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import javax.annotation.Nonnull;

public interface NgPipelineExecutionService {
  PlanExecution runPipeline(String pipelineYaml, String accountId, String orgId, String projectId, EmbeddedUser user);

  NGPipelineExecutionResponseDTO runPipelineWithInputSetPipelineYaml(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String inputSetPipelineYaml, boolean useFQNIfErrorResponse,
      EmbeddedUser user);

  NGPipelineExecutionResponseDTO runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> inputSetReferences,
      boolean useFQNIfErrorResponse, EmbeddedUser user);

  Page<PipelineExecutionSummary> getExecutions(String accountId, String orgId, String projectId, Pageable pageable);

  PipelineExecutionSummary createPipelineExecutionSummary(
      String accountId, String orgId, String projectId, PlanExecution planExecution, NgPipeline ngPipeline);

  PipelineExecutionDetail getPipelineExecutionDetail(@Nonnull String planExecutionId, String stageId);

  PipelineExecutionSummary getByPlanExecutionId(
      String accountId, String orgId, String projectId, String planExecutionId);

  PipelineExecutionSummary updateStatusForGivenNode(
      String accountId, String orgId, String projectId, String planExecutionId, NodeExecution nodeExecution);
}
