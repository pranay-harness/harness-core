package io.harness.pms.plan.execution.handlers;

import static io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup.PIPELINE;
import static io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup.STAGE;

import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.engine.observers.NodeUpdateObserver;
import io.harness.execution.NodeExecution;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.plan.execution.ExecutionSummaryUpdateUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.steps.StepSpecTypeConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@Singleton
public class ExecutionSummaryUpdateEventHandler implements NodeUpdateObserver, AsyncInformObserver {
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRepository;
  @Inject @Named("PipelineExecutorService") private ExecutorService executorService;

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }

  @Override
  public void onNodeUpdate(NodeUpdateInfo nodeUpdateInfo) {
    updateStageLevelInfo(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecution());
    updatePipelineLevelInfo(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecution());
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
    // TODO: Do this better
    if (Objects.equals(nodeExecution.getNode().getGroup(), STAGE.name())
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
