package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.nullIfEmpty;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.APPROVAL_RESUME;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStageExecution;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class PipelineStageExecutionMetadata implements GraphNodeVisitable {
  String stageName;
  String name;
  String type;
  ExecutionStatus status;
  boolean parallelWithPreviousStage;
  SkipConditionMetadata skipCondition;

  // For APPROVAL and APPROVAL_RESUME states.
  ApprovalMetadata approvalData;

  // For ENV_STATE and ENV_RESUME_STATE states.
  WorkflowExecutionMetadata workflowExecution;

  TimingMetadata timing;

  public void accept(GraphNodeVisitor visitor) {
    if (workflowExecution != null) {
      workflowExecution.accept(visitor);
    }
  }

  static List<PipelineStageExecutionMetadata> fromPipelineExecution(PipelineExecution pipelineExecution) {
    if (pipelineExecution == null || pipelineExecution.getPipeline() == null) {
      return null;
    }

    List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    List<PipelineStage> pipelineStages = pipelineExecution.getPipeline().getPipelineStages();
    if (hasNone(pipelineStageExecutions) || hasNone(pipelineStages)) {
      if (hasSome(pipelineStageExecutions) || hasSome(pipelineStages)) {
        throwIncompatibleStagesException(pipelineExecution);
      }

      return null;
    }

    if (pipelineStageExecutions.size() != pipelineStages.size()) {
      throwIncompatibleStagesException(pipelineExecution);
      return null;
    }

    List<PipelineStageExecutionMetadata> pipelineStageExecutionMetadataList = new ArrayList<>();
    boolean firstStage = true;
    for (int i = 0; i < pipelineStageExecutions.size(); i++) {
      PipelineStageExecutionMetadata pipelineStageExecutionMetadata =
          fromPipelineStageExecution(pipelineStageExecutions.get(i), pipelineStages.get(i), firstStage);
      if (pipelineStageExecutionMetadata != null) {
        pipelineStageExecutionMetadataList.add(pipelineStageExecutionMetadata);
      }

      firstStage = false;
    }

    return nullIfEmpty(pipelineStageExecutionMetadataList);
  }

  @VisibleForTesting
  static PipelineStageExecutionMetadata fromPipelineStageExecution(
      PipelineStageExecution pipelineStageExecution, PipelineStage pipelineStage, boolean firstStage) {
    if (pipelineStageExecution == null || pipelineStage == null) {
      return null;
    }

    boolean isApproval = APPROVAL.name().equals(pipelineStageExecution.getStateType())
        || APPROVAL_RESUME.name().equals(pipelineStageExecution.getStateType());
    List<WorkflowExecutionMetadata> workflowExecutionMetadataList = isApproval
        ? null
        : WorkflowExecutionMetadata.fromWorkflowExecutions(pipelineStageExecution.getWorkflowExecutions());
    return PipelineStageExecutionMetadata.builder()
        .stageName(pipelineStage.getName())
        .name(pipelineStageExecution.getStateName())
        .type(isApproval ? "APPROVAL" : "WORKFLOW_EXECUTION")
        .status(pipelineStageExecution.getStatus())
        .parallelWithPreviousStage(!firstStage && pipelineStage.isParallel())
        .skipCondition(SkipConditionMetadata.fromPipelineStageExecution(pipelineStageExecution, pipelineStage))
        .approvalData(
            isApproval ? ApprovalMetadata.fromStateExecutionData(pipelineStageExecution.getStateExecutionData()) : null)
        .workflowExecution(hasNone(workflowExecutionMetadataList) ? null : workflowExecutionMetadataList.get(0))
        .timing(TimingMetadata.fromStartAndEndTimeObjects(
            pipelineStageExecution.getStartTs(), pipelineStageExecution.getEndTs()))
        .build();
  }

  private static void throwIncompatibleStagesException(PipelineExecution pipelineExecution) {
    throw new InvalidRequestException(format(
        "Unable to process export execution request. Pipeline execution [%s] has incompatible stages and stage executions",
        pipelineExecution.getWorkflowExecutionId()));
  }
}
