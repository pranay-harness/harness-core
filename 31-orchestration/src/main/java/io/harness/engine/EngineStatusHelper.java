package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.status.ExecutionInstanceStatus;
import io.harness.persistence.HPersistence;
import lombok.NonNull;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.function.Consumer;

@OwnedBy(CDC)
@Redesign
public class EngineStatusHelper {
  @Inject HPersistence hPersistence;

  public NodeExecution updateNodeInstance(
      String nodeInstanceId, @NonNull Consumer<UpdateOperations<NodeExecution>> ops) {
    Query<NodeExecution> findQuery =
        hPersistence.createQuery(NodeExecution.class).filter(NodeExecutionKeys.uuid, nodeInstanceId);
    UpdateOperations<NodeExecution> operations = hPersistence.createUpdateOperations(NodeExecution.class);
    ops.accept(operations);
    return hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
  }

  public PlanExecution updateExecutionInstanceStatus(String instanceId, ExecutionInstanceStatus status) {
    Query<PlanExecution> findQuery =
        hPersistence.createQuery(PlanExecution.class).filter(NodeExecutionKeys.uuid, instanceId);
    UpdateOperations<PlanExecution> operations =
        hPersistence.createUpdateOperations(PlanExecution.class).set(PlanExecutionKeys.status, status);
    return hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
  }
}
