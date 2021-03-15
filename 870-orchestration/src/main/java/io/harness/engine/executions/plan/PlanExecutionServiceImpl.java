package io.harness.engine.executions.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdateInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.repositories.PlanExecutionRepository;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(CDC)
@Slf4j
public class PlanExecutionServiceImpl implements PlanExecutionService {
  @Inject private PlanExecutionRepository planExecutionRepository;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Override
  public PlanExecution save(PlanExecution planExecution) {
    return planExecutionRepository.save(planExecution);
  }

  /**
   * Always use this method while updating statuses. This guarantees we a hopping from correct statuses.
   * As we don't have transactions it is possible that your execution state is manipulated by some other thread and
   * your transition is no longer valid.
   *
   * Like your workflow is aborted but some other thread try to set it to running. Same logic applied to plan execution
   * status as well
   */
  @Override
  public PlanExecution updateStatus(@NonNull String planExecutionId, @NonNull Status status, Consumer<Update> ops) {
    EnumSet<Status> allowedStartStatuses = StatusUtils.planAllowedStartSet(status);
    Query query = query(where(PlanExecutionKeys.uuid).is(planExecutionId))
                      .addCriteria(where(PlanExecutionKeys.status).in(allowedStartStatuses));
    Update updateOps = new Update()
                           .set(PlanExecutionKeys.status, status)
                           .set(PlanExecutionKeys.lastUpdatedAt, System.currentTimeMillis());
    if (ops != null) {
      ops.accept(updateOps);
    }
    PlanExecution updated = mongoTemplate.findAndModify(
        query, updateOps, new FindAndModifyOptions().upsert(false).returnNew(true), PlanExecution.class);
    if (updated == null) {
      log.warn("Cannot update execution status for the PlanExecution {} with {}", planExecutionId, status);
    } else {
      emitEvent(updated);
    }
    return updated;
  }

  @Override
  public PlanExecution updateStatus(@NonNull String planExecutionId, @NonNull Status status) {
    return updateStatus(planExecutionId, status, null);
  }

  @Override
  public PlanExecution update(@NonNull String planExecutionId, @NonNull Consumer<Update> ops) {
    Query query = query(where(PlanExecutionKeys.uuid).is(planExecutionId));
    Update updateOps = new Update().set(PlanExecutionKeys.lastUpdatedAt, System.currentTimeMillis());
    ops.accept(updateOps);
    PlanExecution updated = mongoTemplate.findAndModify(query, updateOps, PlanExecution.class);
    if (updated == null) {
      throw new InvalidRequestException("Node Execution Cannot be updated with provided operations" + planExecutionId);
    }
    return updated;
  }

  @Override
  public PlanExecution get(String planExecutionId) {
    return planExecutionRepository.findById(planExecutionId)
        .orElseThrow(() -> new InvalidRequestException("Plan Execution is null for id: " + planExecutionId));
  }

  @Override
  public PlanNodeProto fetchExecutionNode(String planExecutionId, String nodeId) {
    PlanExecution instance = get(planExecutionId);
    if (instance == null) {
      throw new InvalidRequestException("Execution Instance is null for id : " + planExecutionId);
    }
    Plan plan = instance.getPlan();
    return plan.fetchNode(nodeId);
  }

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    log.info("State Status Update Callback Fired : {}", stepStatusUpdateInfo);
  }

  private void emitEvent(PlanExecution planExecution) {
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(buildFromPlanExecution(planExecution))
                               .eventType(OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE)
                               .build());
  }

  public Ambiance buildFromPlanExecution(PlanExecution planExecution) {
    return Ambiance.newBuilder()
        .setPlanExecutionId(planExecution.getUuid())
        .putAllSetupAbstractions(
            isEmpty(planExecution.getSetupAbstractions()) ? new HashMap<>() : planExecution.getSetupAbstractions())
        .setMetadata(
            planExecution.getMetadata() == null ? ExecutionMetadata.newBuilder().build() : planExecution.getMetadata())
        .build();
  }
}
