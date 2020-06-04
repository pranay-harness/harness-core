package io.harness.engine.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionServiceImpl implements NodeExecutionService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

  @Override
  public NodeExecution get(String nodeExecutionId) {
    NodeExecution nodeExecution = hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                      .filter(NodeExecutionKeys.uuid, nodeExecutionId)
                                      .get();
    if (nodeExecution == null) {
      throw new InvalidRequestException("Node Execution is null for id: " + nodeExecutionId);
    }
    return nodeExecution;
  }

  @Override
  public List<NodeExecution> fetchNodeExecutions(String planExecutionId) {
    return fetchNodeExecutionsInternal(hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                           .filter(NodeExecutionKeys.planExecutionId, planExecutionId));
  }

  @Override
  public List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId) {
    return fetchNodeExecutionsInternal(hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                           .filter(NodeExecutionKeys.planExecutionId, planExecutionId)
                                           .filter(NodeExecutionKeys.parentId, parentId)
                                           .order(Sort.ascending(NodeExecutionKeys.createdAt)));
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, Status status) {
    return fetchNodeExecutionsInternal(hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                           .filter(NodeExecutionKeys.planExecutionId, planExecutionId)
                                           .filter(NodeExecutionKeys.status, status));
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatuses(String planExecutionId, EnumSet<Status> statuses) {
    return fetchNodeExecutionsInternal(hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                           .filter(NodeExecutionKeys.planExecutionId, planExecutionId)
                                           .field(NodeExecutionKeys.status)
                                           .in(statuses));
  }

  private List<NodeExecution> fetchNodeExecutionsInternal(Query<NodeExecution> nodeExecutionQuery) {
    List<NodeExecution> nodeExecutions = new ArrayList<>();
    try (HIterator<NodeExecution> nodeExecutionIterator = new HIterator<>(nodeExecutionQuery.fetch())) {
      while (nodeExecutionIterator.hasNext()) {
        nodeExecutions.add(nodeExecutionIterator.next());
      }
    }
    return nodeExecutions;
  }

  @Override
  public NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<UpdateOperations<NodeExecution>> ops) {
    Query<NodeExecution> findQuery =
        hPersistence.createQuery(NodeExecution.class).filter(NodeExecutionKeys.uuid, nodeExecutionId);
    UpdateOperations<NodeExecution> operations = hPersistence.createUpdateOperations(NodeExecution.class);
    ops.accept(operations);
    NodeExecution updated = hPersistence.findAndModify(findQuery, operations, HPersistence.returnNewOptions);
    if (updated == null) {
      throw new InvalidRequestException("Node Execution Cannot be updated with provided operations" + nodeExecutionId);
    }
    return updated;
  }

  /**
   * Always use this method while updating statuses. This guarantees we a hopping from correct statuses.
   * As we don't have transactions it is possible that you node execution state is manipulated by some other thread and
   * your transition is no longer valid.
   *
   * Like your workflow is aborted but some other thread try to set it to running. Same logic applied to plan execution
   * status as well
   */

  @Override
  public NodeExecution updateStatusWithOps(
      @NonNull String nodeExecutionId, @NonNull Status status, Consumer<UpdateOperations<NodeExecution>> ops) {
    EnumSet<Status> allowedStartStatuses = Status.obtainAllowedStartSet(status);
    Query<NodeExecution> findQuery = hPersistence.createQuery(NodeExecution.class)
                                         .filter(NodeExecutionKeys.uuid, nodeExecutionId)
                                         .field(NodeExecutionKeys.status)
                                         .in(allowedStartStatuses);
    UpdateOperations<NodeExecution> operations =
        hPersistence.createUpdateOperations(NodeExecution.class).set(NodeExecutionKeys.status, status);
    if (ops != null) {
      ops.accept(operations);
    }
    NodeExecution updated = hPersistence.findAndModify(findQuery, operations, HPersistence.returnNewOptions);
    if (updated == null) {
      logger.warn("Cannot update execution status for the node {} with {}", nodeExecutionId, status);
    }
    return updated;
  }
}
