package io.harness.engine.helpers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.client.result.UpdateResult;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.services.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class RetryHelper {
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private ExecutionEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private MongoTemplate mongoTemplate;

  public void retryNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(nodeExecutionService.get(nodeExecutionId));
    PlanNode node = nodeExecution.getNode();
    NodeExecution newNodeExecution = cloneForRetry(nodeExecution);

    Ambiance ambiance = ambianceHelper.fetchAmbianceForRetry(nodeExecution);
    ambiance.addLevel(Level.builder()
                          .setupId(node.getUuid())
                          .runtimeId(newNodeExecution.getUuid())
                          .stepType(node.getStepType())
                          .identifier(node.getIdentifier())
                          .group(node.getGroup())
                          .build());
    newNodeExecution.setLevels(ambiance.getLevels());
    NodeExecution savedNodeExecution = nodeExecutionService.save(newNodeExecution);
    updateRelationShips(nodeExecution, savedNodeExecution.getUuid());
    updateOldExecution(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(ambiance).executionEngine(engine).build());
  }

  private NodeExecution cloneForRetry(NodeExecution nodeExecution) {
    NodeExecution newNodeExecution = nodeExecution.deepCopy();
    newNodeExecution.setUuid(null);
    newNodeExecution.setStartTs(null);
    newNodeExecution.setStatus(Status.QUEUED);
    List<String> retryIds = isEmpty(nodeExecution.getRetryIds()) ? new ArrayList<>() : nodeExecution.getRetryIds();
    retryIds.add(0, nodeExecution.getUuid());
    newNodeExecution.setRetryIds(retryIds);
    newNodeExecution.setExecutableResponses(new ArrayList<>());
    return nodeExecutionService.save(newNodeExecution);
  }

  private void updateRelationShips(NodeExecution nodeExecution, String newNodeExecutionId) {
    Update ops = new Update().set(NodeExecutionKeys.previousId, newNodeExecutionId);
    Query query = query(where(NodeExecutionKeys.previousId).is(nodeExecution.getUuid()));
    UpdateResult updateResult = mongoTemplate.updateMulti(query, ops, NodeExecution.class);
    if (updateResult.wasAcknowledged()) {
      logger.warn("No previous nodeExecutions could be updated for this nodeExecutionId: {}", nodeExecution.getUuid());
    }
  }

  private void updateOldExecution(NodeExecution nodeExecution) {
    Update ops = new Update().set(NodeExecutionKeys.oldRetry, Boolean.TRUE);
    Query query = query(where(NodeExecutionKeys.uuid).is(nodeExecution.getUuid()));
    mongoTemplate.findAndModify(query, ops, NodeExecution.class);
  }
}
