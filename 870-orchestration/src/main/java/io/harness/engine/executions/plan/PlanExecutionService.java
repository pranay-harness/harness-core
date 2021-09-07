package io.harness.engine.executions.plan;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.NodeStatusUpdateObserver;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.execution.Status;

import java.util.List;
import java.util.function.Consumer;
import lombok.NonNull;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
public interface PlanExecutionService extends NodeStatusUpdateObserver {
  PlanExecution update(@NonNull String planExecutionId, @NonNull Consumer<Update> ops);

  PlanExecution updateStatus(@NonNull String planExecutionId, @NonNull Status status);

  PlanExecution updateStatus(@NonNull String planExecutionId, @NonNull Status status, Consumer<Update> ops);

  PlanExecution get(String planExecutionId);

  PlanExecution save(PlanExecution planExecution);

  List<PlanExecution> findAllByPlanExecutionIdIn(List<String> planExecutionIds);

  List<PlanExecution> findPrevUnTerminatedPlanExecutionsByExecutionTag(
      PlanExecution planExecution, String executionTag);

  Status calculateStatus(String planExecutionId);

  PlanExecution updateCalculatedStatus(String planExecutionId);

  Status calculateStatusExcluding(String planExecutionId, String excludedNodeExecutionId);
}
