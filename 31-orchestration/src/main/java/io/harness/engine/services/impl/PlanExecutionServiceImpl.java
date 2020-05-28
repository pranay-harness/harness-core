package io.harness.engine.services.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.engine.services.PlanExecutionService;
import io.harness.engine.status.StepStatusUpdateInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.persistence.HPersistence;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.function.Consumer;

@Slf4j
public class PlanExecutionServiceImpl implements PlanExecutionService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

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
