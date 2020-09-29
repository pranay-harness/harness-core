package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.execution.status.Status.DISCONTINUING;
import static io.harness.springdata.SpringDataMongoUtils.returnNewOptions;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.client.result.UpdateResult;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.execution.status.Status;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.InterruptEffect;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionServiceImpl implements NodeExecutionService {
  @Inject private NodeExecutionRepository nodeExecutionRepository;
  @Inject @Named("orchestrationMongoTemplate") private MongoTemplate mongoTemplate;
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Override
  public NodeExecution get(String nodeExecutionId) {
    return nodeExecutionRepository.findById(nodeExecutionId)
        .orElseThrow(() -> new InvalidRequestException("Node Execution is null for id: " + nodeExecutionId));
  }

  @Override
  public NodeExecution getByPlanNodeUuid(String planNodeUuid, String planExecutionId) {
    return nodeExecutionRepository.findByNodeUuidAndAmbiancePlanExecutionId(planNodeUuid, planExecutionId)
        .orElseThrow(() -> new InvalidRequestException("Node Execution is null for planNodeUuid: " + planNodeUuid));
  }

  @Override
  public List<NodeExecution> fetchNodeExecutions(String planExecutionId) {
    return nodeExecutionRepository.findByAmbiancePlanExecutionId(planExecutionId);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsWithoutOldRetries(String planExecutionId) {
    return nodeExecutionRepository.findByAmbiancePlanExecutionIdAndOldRetry(planExecutionId, Boolean.FALSE);
  }

  @Override
  public List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId) {
    return nodeExecutionRepository.findByAmbiancePlanExecutionIdAndParentIdOrderByCreatedAtDesc(
        planExecutionId, parentId);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByNotifyId(String planExecutionId, String notifyId) {
    return nodeExecutionRepository.findByAmbiancePlanExecutionIdAndNotifyIdOrderByCreatedAtDesc(
        planExecutionId, notifyId);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, Status status) {
    return nodeExecutionRepository.findByAmbiancePlanExecutionIdAndStatus(planExecutionId, status);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatuses(String planExecutionId, EnumSet<Status> statuses) {
    return nodeExecutionRepository.findByAmbiancePlanExecutionIdAndStatusIn(planExecutionId, statuses);
  }

  @Override
  public NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<Update> ops) {
    Query query = query(where(NodeExecutionKeys.uuid).is(nodeExecutionId));
    Update updateOps = new Update().set(NodeExecutionKeys.lastUpdatedAt, System.currentTimeMillis());
    ops.accept(updateOps);
    NodeExecution updated = mongoTemplate.findAndModify(query, updateOps, returnNewOptions, NodeExecution.class);
    if (updated == null) {
      throw new NodeExecutionUpdateFailedException(
          "Node Execution Cannot be updated with provided operations" + nodeExecutionId);
    }
    return updated;
  }

  @Override
  public List<NodeExecution> fetchChildrenNodeExecutionsByStatuses(
      String planExecutionId, List<String> parentIds, EnumSet<Status> statuses) {
    return nodeExecutionRepository.findByAmbiancePlanExecutionIdAndParentIdInAndStatusIn(
        planExecutionId, parentIds, statuses);
  }

  @Override
  public NodeExecution save(NodeExecution nodeExecution) {
    return nodeExecutionRepository.save(nodeExecution);
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
      @NonNull String nodeExecutionId, @NonNull Status status, Consumer<Update> ops) {
    EnumSet<Status> allowedStartStatuses = Status.nodeAllowedStartSet(status);
    Query query = query(where(NodeExecutionKeys.uuid).is(nodeExecutionId))
                      .addCriteria(where(NodeExecutionKeys.status).in(allowedStartStatuses));
    Update updateOps = new Update()
                           .set(NodeExecutionKeys.status, status)
                           .set(NodeExecutionKeys.lastUpdatedAt, System.currentTimeMillis());
    if (ops != null) {
      ops.accept(updateOps);
    }
    NodeExecution updated = mongoTemplate.findAndModify(query, updateOps, returnNewOptions, NodeExecution.class);
    if (updated == null) {
      logger.warn("Cannot update execution status for the node {} with {}", nodeExecutionId, status);
    } else {
      eventEmitter.emitEvent(OrchestrationEvent.builder()
                                 .ambiance(updated.getAmbiance())
                                 .eventType(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE)
                                 .build());
    }
    return updated;
  }

  @Override
  public boolean markLeavesDiscontinuingOnAbort(
      String interruptId, ExecutionInterruptType interruptType, String planExecutionId, List<String> leafInstanceIds) {
    Update ops = new Update();
    ops.set(NodeExecutionKeys.status, DISCONTINUING);
    ops.addToSet(NodeExecutionKeys.interruptHistories,
        InterruptEffect.builder()
            .interruptId(interruptId)
            .tookEffectAt(System.currentTimeMillis())
            .interruptType(interruptType)
            .build());

    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.uuid).in(leafInstanceIds));
    UpdateResult updateResult = mongoTemplate.updateMulti(query, ops, NodeExecution.class);
    if (!updateResult.wasAcknowledged()) {
      logger.warn("No NodeExecutions could be marked as DISCONTINUING -  planExecutionId: {}", planExecutionId);
      return false;
    }
    return true;
  }

  /**
   * Update the old execution -> set oldRetry flag set to true
   *
   * @param nodeExecutionId Id of Failed Node Execution
   */
  @Override
  public boolean markRetried(String nodeExecutionId) {
    Update ops = new Update().set(NodeExecutionKeys.oldRetry, Boolean.TRUE);
    Query query = query(where(NodeExecutionKeys.uuid).is(nodeExecutionId));
    NodeExecution nodeExecution = mongoTemplate.findAndModify(query, ops, NodeExecution.class);
    if (nodeExecution == null) {
      logger.error("Failed to mark node as retry");
      return false;
    }
    return true;
  }

  /**
   * Update Nodes for which the previousId was failed node execution and replace it with the
   * note execution which is being retried
   *
   * @param nodeExecutionId Old nodeExecutionId
   * @param newNodeExecutionId Id of new retry node execution
   */
  @Override
  public boolean updateRelationShipsForRetryNode(String nodeExecutionId, String newNodeExecutionId) {
    Update ops = new Update().set(NodeExecutionKeys.previousId, newNodeExecutionId);
    Query query = query(where(NodeExecutionKeys.previousId).is(nodeExecutionId));
    UpdateResult updateResult = mongoTemplate.updateMulti(query, ops, NodeExecution.class);
    if (updateResult.wasAcknowledged()) {
      logger.warn("No previous nodeExecutions could be updated for this nodeExecutionId: {}", nodeExecutionId);
      return false;
    }
    return true;
  }

  @Override
  public Optional<NodeExecution> getByNodeIdentifier(String nodeIdentifier, String planExecutionId) {
    return nodeExecutionRepository.findByNodeIdentifierAndAmbiancePlanExecutionId(nodeIdentifier, planExecutionId);
  }
}
