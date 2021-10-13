package io.harness.pms.plan.execution.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.utils.OrchestrationUtils;
import io.harness.execution.NodeExecution;
import io.harness.pms.plan.execution.ExecutionSummaryUpdateUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.steps.StepSpecTypeConstants;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsExecutionSummaryServiceImpl implements PmsExecutionSummaryService {
  @Inject NodeExecutionService nodeExecutionService;
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRepository;

  /**
   * Updates all the fields in the stage graph from nodeExecutions.
   * @param planExecutionId
   */
  @Override
  public void regenerateStageLayoutGraph(String planExecutionId) {
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchStageExecutions(planExecutionId);
    Update update = new Update();
    for (NodeExecution nodeExecution : nodeExecutions) {
      ExecutionSummaryUpdateUtils.addStageUpdateCriteria(update, planExecutionId, nodeExecution);
    }
    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }

  @Override
  public void updateEndTs(String planExecutionId, NodeExecution nodeExecution) {
    Update update = new Update();
    boolean updated = false;

    // Update endTs at pipeline level
    if (OrchestrationUtils.isPipelineNode(nodeExecution)) {
      if (nodeExecution.getEndTs() != null) {
        updated = true;
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.endTs, nodeExecution.getEndTs());
      }
    }

    // Update endTs at stage level
    if (OrchestrationUtils.isStageNode(nodeExecution)) {
      String stageUuid = nodeExecution.getNode().getUuid();
      if (nodeExecution.getEndTs() != null) {
        updated = true;
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".endTs",
            nodeExecution.getEndTs());
      }
    }
    if (updated) {
      Criteria criteria =
          Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }

  @Override
  public void update(String planExecutionId, NodeExecution nodeExecution) {
    updatePipelineLevelInfo(planExecutionId, nodeExecution);
    updateStageLevelInfo(planExecutionId, nodeExecution);
  }

  private void updatePipelineLevelInfo(String planExecutionId, NodeExecution nodeExecution) {
    if (OrchestrationUtils.isPipelineNode(nodeExecution)) {
      Update update = new Update();
      ExecutionSummaryUpdateUtils.addPipelineUpdateCriteria(update, planExecutionId, nodeExecution);
      Criteria criteria =
          Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }

  private void updateStageLevelInfo(String planExecutionId, NodeExecution nodeExecution) {
    if (OrchestrationUtils.isStageNode(nodeExecution)
        || Objects.equals(nodeExecution.getNode().getStepType().getType(), StepSpecTypeConstants.BARRIER)) {
      Update update = new Update();
      ExecutionSummaryUpdateUtils.addStageUpdateCriteria(update, planExecutionId, nodeExecution);
      Criteria criteria =
          Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }
}
