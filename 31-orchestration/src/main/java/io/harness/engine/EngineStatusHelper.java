package io.harness.engine;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.persistence.HPersistence;
import io.harness.state.execution.ExecutionInstance;
import io.harness.state.execution.ExecutionInstance.ExecutionInstanceKeys;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.execution.ExecutionNodeInstance.ExecutionNodeInstanceKeys;
import io.harness.state.execution.status.ExecutionInstanceStatus;
import io.harness.state.execution.status.NodeExecutionStatus;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.function.Consumer;

@Redesign
public class EngineStatusHelper {
  @Inject HPersistence hPersistence;

  public ExecutionNodeInstance updateNodeInstance(
      String nodeInstanceId, NodeExecutionStatus status, Consumer<UpdateOperations<ExecutionNodeInstance>> ops) {
    Query<ExecutionNodeInstance> findQuery =
        hPersistence.createQuery(ExecutionNodeInstance.class).filter(ExecutionNodeInstanceKeys.uuid, nodeInstanceId);
    UpdateOperations<ExecutionNodeInstance> operations =
        hPersistence.createUpdateOperations(ExecutionNodeInstance.class).set(ExecutionNodeInstanceKeys.status, status);
    if (ops != null) {
      ops.accept(operations);
    }
    return hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
  }

  public ExecutionInstance updateExecutionInstanceStatus(String instanceId, ExecutionInstanceStatus status) {
    Query<ExecutionInstance> findQuery =
        hPersistence.createQuery(ExecutionInstance.class).filter(ExecutionNodeInstanceKeys.uuid, instanceId);
    UpdateOperations<ExecutionInstance> operations =
        hPersistence.createUpdateOperations(ExecutionInstance.class).set(ExecutionInstanceKeys.status, status);
    return hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
  }
}
