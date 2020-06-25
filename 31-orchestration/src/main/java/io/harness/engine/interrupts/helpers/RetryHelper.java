package io.harness.engine.interrupts.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.client.result.UpdateResult;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
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

@OwnedBy(CDC)
@Slf4j
public class RetryHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private MongoTemplate mongoTemplate;

  public void retryNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(nodeExecutionService.get(nodeExecutionId));
    PlanNode node = nodeExecution.getNode();
    String newUuid = generateUuid();
    NodeExecution newNodeExecution = cloneForRetry(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance().cloneForFinish();
    ambiance.addLevel(Level.builder()
                          .setupId(node.getUuid())
                          .runtimeId(newUuid)
                          .stepType(node.getStepType())
                          .identifier(node.getIdentifier())
                          .group(node.getGroup())
                          .build());
    newNodeExecution.setUuid(newUuid);
    newNodeExecution.setAmbiance(ambiance);
    NodeExecution savedNodeExecution = nodeExecutionService.save(newNodeExecution);
    updateRelationShips(nodeExecution, savedNodeExecution.getUuid());
    updateOldExecution(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(ambiance).orchestrationEngine(engine).build());
  }

  private NodeExecution cloneForRetry(NodeExecution nodeExecution) {
    NodeExecution newNodeExecution = nodeExecution.deepCopy();
    newNodeExecution.setStartTs(null);
    newNodeExecution.setStatus(Status.QUEUED);
    List<String> retryIds = isEmpty(nodeExecution.getRetryIds()) ? new ArrayList<>() : nodeExecution.getRetryIds();
    retryIds.add(0, nodeExecution.getUuid());
    newNodeExecution.setRetryIds(retryIds);
    newNodeExecution.setExecutableResponses(new ArrayList<>());
    newNodeExecution.setVersion(null);
    return newNodeExecution;
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
