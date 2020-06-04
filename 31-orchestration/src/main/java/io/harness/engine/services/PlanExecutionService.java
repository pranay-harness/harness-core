package io.harness.engine.services;

import io.harness.engine.status.StepStatusUpdate;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import lombok.NonNull;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.function.Consumer;

public interface PlanExecutionService extends StepStatusUpdate {
  PlanExecution update(@NonNull String planExecutionId, @NonNull Consumer<UpdateOperations<PlanExecution>> ops);

  PlanExecution updateStatusWithOps(
      @NonNull String planExecutionId, @NonNull Status status, Consumer<UpdateOperations<PlanExecution>> ops);

  PlanExecution get(String planExecutionId);
}
