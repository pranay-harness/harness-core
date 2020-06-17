package io.harness.engine.executions.plan;

import io.harness.engine.status.StepStatusUpdate;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import lombok.NonNull;
import org.springframework.data.mongodb.core.query.Update;

import java.util.function.Consumer;

public interface PlanExecutionService extends StepStatusUpdate {
  PlanExecution update(@NonNull String planExecutionId, @NonNull Consumer<Update> ops);

  PlanExecution updateStatus(@NonNull String planExecutionId, @NonNull Status status);

  PlanExecution updateStatusWithOps(@NonNull String planExecutionId, @NonNull Status status, Consumer<Update> ops);

  PlanExecution get(String planExecutionId);

  PlanExecution save(PlanExecution planExecution);
}
