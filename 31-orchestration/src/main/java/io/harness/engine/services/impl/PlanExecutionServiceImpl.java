package io.harness.engine.services.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.engine.services.PlanExecutionService;
import io.harness.engine.status.StepStatusUpdateInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.persistence.HPersistence;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.EnumSet;
import java.util.function.Consumer;

@Slf4j
public class PlanExecutionServiceImpl implements PlanExecutionService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

  /**
   * Always use this method while updating statuses. This guarantees we a hopping from correct statuses.
   * As we don't have transactions it is possible that your execution state is manipulated by some other thread and
   * your transition is no longer valid.
   *
   * Like your workflow is aborted but some other thread try to set it to running. Same logic applied to plan execution
   * status as well
   */
  @Override
  public PlanExecution updateStatusWithOps(
      @NonNull String planExecutionId, @NonNull Status status, Consumer<UpdateOperations<PlanExecution>> ops) {
    EnumSet<Status> allowedStartStatuses = Status.obtainAllowedStartSet(status);
    Query<PlanExecution> findQuery = hPersistence.createQuery(PlanExecution.class)
                                         .filter(PlanExecutionKeys.uuid, planExecutionId)
                                         .field(PlanExecutionKeys.status)
                                         .in(allowedStartStatuses);
    UpdateOperations<PlanExecution> operations =
        hPersistence.createUpdateOperations(PlanExecution.class).set(PlanExecutionKeys.status, status);
    if (ops != null) {
      ops.accept(operations);
    }
    PlanExecution updated = hPersistence.findAndModify(findQuery, operations, HPersistence.returnNewOptions);
    if (updated == null) {
      logger.warn("Cannot update execution status for the node {} with {}", planExecutionId, status);
    }
    return updated;
  }

  @Override
  public PlanExecution update(String planExecutionId, @NonNull Consumer<UpdateOperations<PlanExecution>> ops) {
    Query<PlanExecution> findQuery =
        hPersistence.createQuery(PlanExecution.class).filter(PlanExecutionKeys.uuid, planExecutionId);
    UpdateOperations<PlanExecution> operations = hPersistence.createUpdateOperations(PlanExecution.class);
    ops.accept(operations);
    PlanExecution updated = hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
    if (updated == null) {
      throw new InvalidRequestException("Node Execution Cannot be updated with provided operations" + planExecutionId);
    }
    return updated;
  }

  @Override
  public PlanExecution get(String planExecutionId) {
    PlanExecution planExecution = hPersistence.createQuery(PlanExecution.class, excludeAuthority)
                                      .filter(PlanExecutionKeys.uuid, planExecutionId)
                                      .get();
    if (planExecution == null) {
      throw new InvalidRequestException("Plan Execution is null for id: " + planExecutionId);
    }
    return planExecution;
  }

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    logger.info("State Status Update Callback Fired : {}", stepStatusUpdateInfo);
  }
}
