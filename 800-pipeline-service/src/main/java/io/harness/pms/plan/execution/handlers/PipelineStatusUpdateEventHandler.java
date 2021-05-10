package io.harness.pms.plan.execution.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.inject.Inject;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineStatusUpdateEventHandler implements SyncOrchestrationEventHandler {
  @Inject PlanExecutionService planExecutionService;
  @Inject PmsExecutionSummaryRespository pmsExecutionSummaryRepository;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String planExecutionId = ambiance.getPlanExecutionId();
    PlanExecution planExecution = planExecutionService.get(planExecutionId);

    ExecutionStatus status = ExecutionStatus.getExecutionStatus(planExecution.getStatus());

    Update update = new Update();

    update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.internalStatus, planExecution.getStatus());
    update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.status, status);
    if (ExecutionStatus.isTerminal(status)) {
      update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.endTs, planExecution.getEndTs());
    }

    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }
}
