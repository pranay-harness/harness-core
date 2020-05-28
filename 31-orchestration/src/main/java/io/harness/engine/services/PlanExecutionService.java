package io.harness.engine.services;

import io.harness.engine.status.StepStatusUpdate;
import io.harness.execution.PlanExecution;
import lombok.NonNull;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.function.Consumer;

public interface PlanExecutionService extends StepStatusUpdate {
  PlanExecution update(String planExecutionId, @NonNull Consumer<UpdateOperations<PlanExecution>> ops);

  PlanExecution get(String planExecutionId);
}
