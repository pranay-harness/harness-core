package io.harness.pms.plan.execution;

import static io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup.PIPELINE;
import static io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup.STAGE;

import io.harness.beans.ExecutionErrorInfo;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.steps.StepSpecTypeConstants;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.mongodb.core.query.Update;

public class ExecutionSummaryUpdateUtils {
  public static void addStageUpdateCriteria(Update update, String planExecutionId, NodeExecution nodeExecution) {
    String stageUuid = nodeExecution.getNode().getUuid();
    ExecutionStatus status = ExecutionStatus.getExecutionStatus(nodeExecution.getStatus());
    if (Objects.equals(nodeExecution.getNode().getGroup(), STAGE.name())) {
      update.set(
          PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".status", status);
      update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".startTs",
          nodeExecution.getStartTs());
      update.set(
          PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".nodeRunInfo",
          nodeExecution.getNodeRunInfo());
      if (ExecutionStatus.isTerminal(status)) {
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".endTs",
            nodeExecution.getEndTs());
      }
      if (status == ExecutionStatus.FAILED) {
        update.set(
            PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".failureInfo",
            ExecutionErrorInfo.builder().message(nodeExecution.getFailureInfo().getErrorMessage()).build());
      }
      if (status == ExecutionStatus.SKIPPED) {
        update.set(
            PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".skipInfo",
            nodeExecution.getSkipInfo());
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".endTs",
            nodeExecution.getEndTs());
      }
    }

    if (Objects.equals(nodeExecution.getNode().getStepType().getType(), StepSpecTypeConstants.BARRIER)) {
      List<Level> levelsList = nodeExecution.getAmbiance().getLevelsList();
      Optional<Level> stage = levelsList.stream().filter(level -> level.getGroup().equals(STAGE.name())).findFirst();
      stage.ifPresent(stageNode
          -> update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "."
                  + stageNode.getSetupId() + ".barrierFound",
              true));
    }
  }

  public static void addPipelineUpdateCriteria(Update update, String planExecutionId, NodeExecution nodeExecution) {
    if (Objects.equals(nodeExecution.getNode().getGroup(), PIPELINE.name())) {
      ExecutionStatus status = ExecutionStatus.getExecutionStatus(nodeExecution.getStatus());
      update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.internalStatus, nodeExecution.getStatus());
      update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.status, status);
      if (ExecutionStatus.isTerminal(status)) {
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.endTs, nodeExecution.getEndTs());
      }
      if (status == ExecutionStatus.FAILED) {
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.executionErrorInfo,
            ExecutionErrorInfo.builder().message(nodeExecution.getFailureInfo().getErrorMessage()).build());
      }
    }
  }
}
