package io.harness.engine.executions.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdate;
import io.harness.execution.PlanExecution;
import io.harness.pms.execution.Status;

import java.util.function.Consumer;
import lombok.NonNull;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(CDC)
public interface PlanExecutionService extends StepStatusUpdate {
  PlanExecution update(@NonNull String planExecutionId, @NonNull Consumer<Update> ops);

  PlanExecution updateStatus(@NonNull String planExecutionId, @NonNull Status status);

  PlanExecution updateStatus(@NonNull String planExecutionId, @NonNull Status status, Consumer<Update> ops);

  PlanExecution get(String planExecutionId);

  PlanExecution save(PlanExecution planExecution);
}
