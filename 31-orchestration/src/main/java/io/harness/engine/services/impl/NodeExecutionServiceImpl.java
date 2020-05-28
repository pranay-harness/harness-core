package io.harness.engine.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.state.io.StepParameters;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
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
  public NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<UpdateOperations<NodeExecution>> ops) {
    Query<NodeExecution> findQuery =
        hPersistence.createQuery(NodeExecution.class).filter(NodeExecutionKeys.uuid, nodeExecutionId);
    UpdateOperations<NodeExecution> operations = hPersistence.createUpdateOperations(NodeExecution.class);
    ops.accept(operations);
    NodeExecution updated = hPersistence.findAndModify(findQuery, operations, HPersistence.upsertReturnNewOptions);
    if (updated == null) {
      throw new InvalidRequestException("Node Execution Cannot be updated with provided operations" + nodeExecutionId);
    }
    return updated;
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, NodeExecutionStatus status) {
    List<NodeExecution> nodeExecutions = new ArrayList<>();
    Query<NodeExecution> nodeExecutionQuery = hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                                  .filter(NodeExecutionKeys.planExecutionId, planExecutionId)
                                                  .filter(NodeExecutionKeys.status, status);
    try (HIterator<NodeExecution> nodeExecutionIterator = new HIterator<>(nodeExecutionQuery.fetch())) {
      while (nodeExecutionIterator.hasNext()) {
        nodeExecutions.add(nodeExecutionIterator.next());
      }
    }
    return nodeExecutions;
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatuses(
      String planExecutionId, EnumSet<NodeExecutionStatus> statuses) {
    List<NodeExecution> nodeExecutions = new ArrayList<>();
    Query<NodeExecution> nodeExecutionQuery = hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                                  .filter(NodeExecutionKeys.planExecutionId, planExecutionId)
                                                  .field(NodeExecutionKeys.status)
                                                  .in(statuses);
    try (HIterator<NodeExecution> nodeExecutionIterator = new HIterator<>(nodeExecutionQuery.fetch())) {
      while (nodeExecutionIterator.hasNext()) {
        nodeExecutions.add(nodeExecutionIterator.next());
      }
    }
    return nodeExecutions;
  }

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
    List<NodeExecution> nodeExecutions = new ArrayList<>();
    Query<NodeExecution> nodeExecutionQuery = hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                                  .filter(NodeExecutionKeys.planExecutionId, planExecutionId);
    try (HIterator<NodeExecution> nodeExecutionIterator = new HIterator<>(nodeExecutionQuery.fetch())) {
      while (nodeExecutionIterator.hasNext()) {
        nodeExecutions.add(nodeExecutionIterator.next());
      }
    }
    return nodeExecutions;
  }

  @Override
  public void updateResolvedStepParameters(String nodeExecutionId, StepParameters stepParameters) {
    Query<NodeExecution> query =
        hPersistence.createQuery(NodeExecution.class, excludeAuthority).filter(NodeExecutionKeys.uuid, nodeExecutionId);
    UpdateOperations<NodeExecution> updateOperations = hPersistence.createUpdateOperations(NodeExecution.class);
    setUnset(updateOperations, NodeExecutionKeys.resolvedStepParameters, stepParameters);
    hPersistence.update(query, updateOperations);
  }
}
