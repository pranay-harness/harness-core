package io.harness.pms.plan.execution.handlers;

import static io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup.PIPELINE;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.engine.observers.NodeUpdateObserver;
import io.harness.execution.NodeExecution;
import io.harness.pms.plan.execution.ExecutionSummaryUpdateUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@Singleton
public class ExecutionSummaryUpdateEventHandler implements NodeUpdateObserver {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRepository;

  @Override
  public void onNodeUpdate(NodeUpdateInfo nodeUpdateInfo) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeUpdateInfo.getNodeExecutionId());
    updateStageLevelInfo(nodeUpdateInfo.getPlanExecutionId(), nodeExecution);
    updatePipelineLevelInfo(nodeUpdateInfo.getPlanExecutionId(), nodeExecution);
  }

  public void updatePipelineLevelInfo(String planExecutionId, NodeExecution nodeExecution) {
    if (Objects.equals(nodeExecution.getNode().getGroup(), PIPELINE)) {
      Update update = new Update();
      ExecutionSummaryUpdateUtils.addPipelineUpdateCriteria(update, planExecutionId, nodeExecution);
      Criteria criteria =
          Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }

  public void updateStageLevelInfo(String planExecutionId, NodeExecution nodeExecution) {
    Update update = new Update();
    ExecutionSummaryUpdateUtils.addStageUpdateCriteria(update, planExecutionId, nodeExecution);
    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }
}
