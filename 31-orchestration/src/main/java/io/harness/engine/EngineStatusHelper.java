package io.harness.engine;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.persistence.HPersistence;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.execution.ExecutionNodeInstance.ExecutionNodeInstanceKeys;
import io.harness.state.execution.PlanExecution;
import io.harness.state.execution.PlanExecution.PlanExecutionKeys;
import io.harness.state.execution.status.ExecutionInstanceStatus;
import lombok.NonNull;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.function.Consumer;

@Redesign
public class EngineStatusHelper {
  @Inject HPersistence hPersistence;

  public ExecutionNodeInstance updateNodeInstance(
      String nodeInstanceId, @NonNull Consumer<UpdateOperations<ExecutionNodeInstance>> ops) {
    Query<ExecutionNodeInstance> findQuery =
        hPersistence.createQuery(ExecutionNodeInstance.class).filter(ExecutionNodeInstanceKeys.uuid, nodeInstanceId);
    UpdateOperations<ExecutionNodeInstance> operations =
        hPersistence.createUpdateOperations(ExecutionNodeInstance.class);
    ops.accept(operations);
    return hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
  }

  public PlanExecution updateExecutionInstanceStatus(String instanceId, ExecutionInstanceStatus status) {
    Query<PlanExecution> findQuery =
        hPersistence.createQuery(PlanExecution.class).filter(ExecutionNodeInstanceKeys.uuid, instanceId);
    UpdateOperations<PlanExecution> operations =
        hPersistence.createUpdateOperations(PlanExecution.class).set(PlanExecutionKeys.status, status);
    return hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
  }
}
