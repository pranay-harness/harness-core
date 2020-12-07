package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.execution.Status.DISCONTINUING;
import static io.harness.springdata.SpringDataMongoUtils.returnNewOptions;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.NodeExecutionMapper;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.pms.steps.StepType;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionServiceImpl implements NodeExecutionService {
  @Inject private MongoTemplate mongoTemplate;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private StepRegistry stepRegistry;

  @Override
  public NodeExecution get(String nodeExecutionId) {
    Query query = query(where(NodeExecutionKeys.uuid).is(nodeExecutionId));
    NodeExecution nodeExecution = mongoTemplate.findOne(query, NodeExecution.class);
    if (nodeExecution == null) {
      throw new InvalidRequestException("Node Execution is null for id: " + nodeExecutionId);
    }
    return nodeExecution;
  }

  // TODO (alexi) : Handle the case where multiple instances are returned
  @Override
  public NodeExecution getByPlanNodeUuid(String planNodeUuid, String planExecutionId) {
    Query query = query(where(NodeExecutionKeys.planNodeId).is(planNodeUuid))
                      .addCriteria(where(NodeExecutionKeys.planExecutionId).is(planExecutionId));
    NodeExecution nodeExecution = mongoTemplate.findOne(query, NodeExecution.class);
    if (nodeExecution == null) {
      throw new InvalidRequestException("Node Execution is null for planNodeUuid: " + planNodeUuid);
    }
    return nodeExecution;
  }

  @Override
  public Optional<NodeExecution> getByNodeIdentifier(String nodeIdentifier, String planExecutionId) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.planNodeIdentifier).in(nodeIdentifier));
    return Optional.ofNullable(mongoTemplate.findOne(query, NodeExecution.class));
  }

  @Override
  public List<NodeExecution> findByParentIdAndStatusIn(String parentId, EnumSet<Status> flowingStatuses) {
    Query query = query(where(NodeExecutionKeys.parentId).is(parentId))
                      .addCriteria(where(NodeExecutionKeys.status).in(flowingStatuses));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutions(String planExecutionId) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsWithoutOldRetries(String planExecutionId) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.oldRetry).is(false));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.parentId).is(parentId))
                      .with(Sort.by(Direction.DESC, NodeExecutionKeys.createdAt));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByNotifyId(String planExecutionId, String notifyId) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.notifyId).is(notifyId))
                      .with(Sort.by(Direction.DESC, NodeExecutionKeys.createdAt));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, Status status) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.status).is(status));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatuses(String planExecutionId, EnumSet<Status> statuses) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.status).in(statuses));
    return mongoTemplate.find(query, NodeExecution.class);
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

    emitEvent(updated.getAmbiance());
    return updated;
  }

  @Override
  public List<NodeExecution> fetchChildrenNodeExecutionsByStatuses(
      String planExecutionId, List<String> parentIds, EnumSet<Status> statuses) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.parentId).in(parentIds))
                      .addCriteria(where(NodeExecutionKeys.status).in(statuses));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public NodeExecution save(NodeExecution nodeExecution) {
    return nodeExecution.getVersion() == null ? mongoTemplate.insert(nodeExecution) : mongoTemplate.save(nodeExecution);
  }

  @Override
  public NodeExecution save(NodeExecutionProto nodeExecution) {
    return save(NodeExecutionMapper.fromNodeExecutionProto(nodeExecution));
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
    EnumSet<Status> allowedStartStatuses = StatusUtils.nodeAllowedStartSet(status);
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
      log.warn("Cannot update execution status for the node {} with {}", nodeExecutionId, status);
    } else {
      emitEvent(updated.getAmbiance());
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
      log.warn("No NodeExecutions could be marked as DISCONTINUING -  planExecutionId: {}", planExecutionId);
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
      log.error("Failed to mark node as retry");
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
      log.warn("No previous nodeExecutions could be updated for this nodeExecutionId: {}", nodeExecutionId);
      return false;
    }
    return true;
  }

  @Override
  public StepParameters extractStepParameters(NodeExecutionProto nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getNode().getStepParameters());
  }

  @Override
  public StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getResolvedStepParameters());
  }

  @Override
  public StepParameters extractStepParameters(NodeExecution nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getNode().getStepParameters());
  }

  @Override
  public StepParameters extractResolvedStepParameters(NodeExecution nodeExecution) {
    return extractStepParametersInternal(nodeExecution.getNode().getStepType(),
        nodeExecution.getResolvedStepParameters() == null ? null : nodeExecution.getResolvedStepParameters().toJson());
  }

  private StepParameters extractStepParametersInternal(StepType stepType, String stepParameters) {
    Step<?> step = stepRegistry.obtain(stepType);
    if (isEmpty(stepParameters)) {
      return null;
    }
    return JsonOrchestrationUtils.asObject(stepParameters, step.getStepParametersClass());
  }

  private void emitEvent(Ambiance ambiance) {
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(ambiance)
                               .eventType(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE)
                               .build());
  }
}
