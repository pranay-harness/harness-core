package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.contracts.execution.Status.DISCONTINUING;
import static io.harness.springdata.SpringDataMongoUtils.returnNewOptions;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdate;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdateInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.NodeExecutionMapper;
import io.harness.observer.Subject;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
@Slf4j
public class NodeExecutionServiceImpl implements NodeExecutionService {
  @Inject private MongoTemplate mongoTemplate;
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Getter private final Subject<StepStatusUpdate> stepStatusUpdateSubject = new Subject<>();

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
                      .addCriteria(where(NodeExecutionKeys.status).in(flowingStatuses))
                      .addCriteria(where(NodeExecutionKeys.oldRetry).is(false));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutions(String planExecutionId) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsWithoutOldRetries(String planExecutionId) {
    return fetchNodeExecutionsWithoutOldRetriesAndStatusIn(planExecutionId, EnumSet.noneOf(Status.class));
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsWithoutOldRetriesAndStatusIn(
      String planExecutionId, EnumSet<Status> statuses) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.oldRetry).is(false));
    if (isNotEmpty(statuses)) {
      query.addCriteria(where(NodeExecutionKeys.status).in(statuses));
    }
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
  public List<NodeExecution> fetchNodeExecutionsByNotifyId(
      String planExecutionId, String notifyId, boolean isOldRetry) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.notifyId).is(notifyId))
                      .addCriteria(where(NodeExecutionKeys.oldRetry).is(isOldRetry))
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

    emitEvent(updated, OrchestrationEventType.NODE_EXECUTION_UPDATE);
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
    if (nodeExecution.getVersion() == null) {
      eventEmitter.emitEvent(OrchestrationEvent.builder()
                                 .ambiance(nodeExecution.getAmbiance())
                                 .nodeExecutionProto(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                 .eventType(OrchestrationEventType.NODE_EXECUTION_START)
                                 .build());
      return mongoTemplate.insert(nodeExecution);
    } else {
      return mongoTemplate.save(nodeExecution);
    }
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
  public NodeExecution updateStatusWithOps(@NonNull String nodeExecutionId, @NonNull Status status,
      Consumer<Update> ops, EnumSet<Status> overrideStatusSet) {
    EnumSet<Status> allowedStartStatuses =
        isEmpty(overrideStatusSet) ? StatusUtils.nodeAllowedStartSet(status) : overrideStatusSet;
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
      emitEvent(updated, OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE);
      stepStatusUpdateSubject.fireInform(StepStatusUpdate::onStepStatusUpdate,
          StepStatusUpdateInfo.builder()
              .nodeExecutionId(updated.getUuid())
              .planExecutionId(updated.getAmbiance().getPlanExecutionId())
              .status(updated.getStatus())
              .build());
    }
    return updated;
  }

  @Override
  public boolean markLeavesDiscontinuingOnAbort(
      String interruptId, InterruptType interruptType, String planExecutionId, List<String> leafInstanceIds) {
    Update ops = new Update();
    ops.set(NodeExecutionKeys.status, DISCONTINUING);
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
    emitEvent(nodeExecution, OrchestrationEventType.NODE_EXECUTION_UPDATE);
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
  public List<NodeExecution> fetchNodeExecutionsByParentId(String nodeExecutionId, boolean oldRetry) {
    Query query = query(where(NodeExecutionKeys.parentId).is(nodeExecutionId))
                      .addCriteria(where(NodeExecutionKeys.oldRetry).is(false));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatusAndIdIn(
      String planExecutionId, Status status, List<String> targetIds) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(NodeExecutionKeys.status).is(status))
                      .addCriteria(where(NodeExecutionKeys.uuid).in(targetIds));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  @Override
  public List<NodeExecution> findAllChildrenWithStatusIn(
      String planExecutionId, String parentId, EnumSet<Status> flowingStatuses, boolean includeParent) {
    List<NodeExecution> finalList = new ArrayList<>();
    List<NodeExecution> allExecutions =
        fetchNodeExecutionsWithoutOldRetriesAndStatusIn(planExecutionId, flowingStatuses);
    return extractChildExecutions(parentId, includeParent, finalList, allExecutions);
  }

  private List<NodeExecution> extractChildExecutions(
      String parentId, boolean includeParent, List<NodeExecution> finalList, List<NodeExecution> allExecutions) {
    Map<String, List<NodeExecution>> parentChildrenMap = new HashMap<>();
    for (NodeExecution execution : allExecutions) {
      if (execution.getParentId() == null) {
        parentChildrenMap.put(execution.getUuid(), new ArrayList<>());
      } else if (parentChildrenMap.containsKey(execution.getParentId())) {
        parentChildrenMap.get(execution.getParentId()).add(execution);
      } else {
        List<NodeExecution> cList = new ArrayList<>();
        cList.add(execution);
        parentChildrenMap.put(execution.getParentId(), cList);
      }
    }
    extractChildList(parentChildrenMap, parentId, finalList);
    if (includeParent) {
      finalList.add(allExecutions.stream()
                        .filter(ne -> ne.getUuid().equals(parentId))
                        .findFirst()
                        .orElseThrow(() -> new UnexpectedException("Expected parent to be in list")));
    }
    return finalList;
  }

  private void extractChildList(
      Map<String, List<NodeExecution>> parentChildrenMap, String parentId, List<NodeExecution> finalList) {
    List<NodeExecution> children = parentChildrenMap.get(parentId);
    if (isEmpty(children)) {
      return;
    }
    finalList.addAll(children);
    children.forEach(child -> extractChildList(parentChildrenMap, child.getUuid(), finalList));
  }

  private void emitEvent(NodeExecution nodeExecution, OrchestrationEventType orchestrationEventType) {
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(nodeExecution.getAmbiance())
                               .nodeExecutionProto(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                               .eventType(orchestrationEventType)
                               .build());
  }
}
