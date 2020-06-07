package io.harness.engine.helpers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.services.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.persistence.HPersistence;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class RetryHelper {
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private ExecutionEngine engine;
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  public void retryNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(nodeExecutionService.get(nodeExecutionId));
    PlanNode node = nodeExecution.getNode();
    String newUuid = generateUuid();
    Ambiance ambiance = ambianceHelper.fetchAmbianceForRetry(nodeExecution);
    ambiance.addLevel(Level.builder()
                          .setupId(node.getUuid())
                          .runtimeId(newUuid)
                          .stepType(node.getStepType())
                          .identifier(node.getIdentifier())
                          .group(node.getGroup())
                          .build());
    NodeExecution newNodeExecution = cloneForRetry(nodeExecution);
    newNodeExecution.setUuid(newUuid);
    newNodeExecution.setLevels(ambiance.getLevels());
    String savedNodeExecutionId = hPersistence.save(newNodeExecution);
    updateRelationShips(nodeExecution, savedNodeExecutionId);
    updateOldExecution(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(ambiance).executionEngine(engine).build());
  }

  private NodeExecution cloneForRetry(NodeExecution nodeExecution) {
    NodeExecution newNodeExecution = nodeExecution.deepCopy();
    newNodeExecution.setStartTs(null);
    newNodeExecution.setStatus(Status.QUEUED);
    List<String> retryIds = isEmpty(nodeExecution.getRetryIds()) ? new ArrayList<>() : nodeExecution.getRetryIds();
    retryIds.add(0, nodeExecution.getUuid());
    newNodeExecution.setRetryIds(retryIds);
    newNodeExecution.setExecutableResponses(new ArrayList<>());
    return newNodeExecution;
  }

  private void updateRelationShips(NodeExecution nodeExecution, String newNodeExecutionId) {
    UpdateOperations<NodeExecution> ops =
        hPersistence.createUpdateOperations(NodeExecution.class).set(NodeExecutionKeys.previousId, newNodeExecutionId);
    Query<NodeExecution> query = hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                     .filter(NodeExecutionKeys.previousId, nodeExecution.getUuid());
    UpdateResults updateResult = hPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() == 0) {
      logger.warn("No previous nodeExecutions could be updated for this nodeExecutionId: {}", nodeExecution.getUuid());
    }
  }

  private void updateOldExecution(NodeExecution nodeExecution) {
    UpdateOperations<NodeExecution> ops =
        hPersistence.createUpdateOperations(NodeExecution.class).set(NodeExecutionKeys.oldRetry, Boolean.TRUE);
    Query<NodeExecution> query = hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                     .filter(NodeExecutionKeys.uuid, nodeExecution.getUuid());
    UpdateResults updateResult = hPersistence.update(query, ops);
    if (updateResult == null || updateResult.getWriteResult() == null || updateResult.getWriteResult().getN() == 0) {
      logger.warn("No previous nodeExecutions could be updated for this nodeExecutionId: {}", nodeExecution.getUuid());
    }
  }
}
