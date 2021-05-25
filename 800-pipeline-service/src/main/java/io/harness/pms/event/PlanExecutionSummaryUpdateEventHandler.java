package io.harness.pms.event;

import static io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup.STAGE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionErrorInfo;
import io.harness.engine.interrupts.statusupdate.NodeExecutionUpdate;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PlanExecutionSummaryUpdateEventHandler implements NodeExecutionUpdate {
  @Inject PmsExecutionSummaryRespository pmsExecutionSummaryRepository;

  @Override
  public void onUpdate(OrchestrationEventType eventType, NodeExecution nodeExecution) {
    if (eventType != OrchestrationEventType.NODE_EXECUTION_UPDATE) {
      return;
    }

    if (!Objects.equals(nodeExecution.getNode().getGroup(), STAGE)) {
      return;
    }
    String stageUuid = nodeExecution.getUuid();
    ExecutionStatus status = ExecutionStatus.getExecutionStatus(nodeExecution.getStatus());
    Update update = new Update();
    update.set(
        PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".status", status);
    update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".startTs",
        nodeExecution.getStartTs());
    update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".nodeRunInfo",
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
      update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".skipInfo",
          nodeExecution.getSkipInfo());
      update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".endTs",
          nodeExecution.getEndTs());
    }
    Criteria criteria = Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId)
                            .is(nodeExecution.getAmbiance().getPlanExecutionId());
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }
}
