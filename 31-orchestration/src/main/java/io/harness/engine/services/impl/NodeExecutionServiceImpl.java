package io.harness.engine.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.ng.SpringDataMongoUtils.returnNewOptions;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.NodeExecutionUpdateFailedException;
import io.harness.engine.services.repositories.NodeExecutionRepository;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionServiceImpl implements NodeExecutionService {
  @Inject private NodeExecutionRepository nodeExecutionRepository;
  @Inject private MongoTemplate mongoTemplate;

  @Override
  public NodeExecution get(String nodeExecutionId) {
    return nodeExecutionRepository.findById(nodeExecutionId)
        .orElseThrow(() -> new InvalidRequestException("Node Execution is null for id: " + nodeExecutionId));
  }

  @Override
  public List<NodeExecution> fetchNodeExecutions(String planExecutionId) {
    return nodeExecutionRepository.findByPlanExecutionId(planExecutionId);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsWithoutOldRetries(String planExecutionId) {
    return nodeExecutionRepository.findByPlanExecutionIdAndOldRetry(planExecutionId, Boolean.FALSE);
  }

  @Override
  public List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId) {
    return nodeExecutionRepository.findByPlanExecutionIdAndParentIdOrderByCreatedAtDesc(planExecutionId, parentId);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, Status status) {
    return nodeExecutionRepository.findByPlanExecutionIdAndStatus(planExecutionId, status);
  }

  @Override
  public List<NodeExecution> fetchNodeExecutionsByStatuses(String planExecutionId, EnumSet<Status> statuses) {
    return nodeExecutionRepository.findByPlanExecutionIdAndStatusIn(planExecutionId, statuses);
  }

  @Override
  public NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<Update> ops) {
    Query query = query(where(NodeExecutionKeys.uuid).is(nodeExecutionId));
    Update updateOps = new Update();
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
    return nodeExecutionRepository.findByPlanExecutionIdAndParentIdInAndStatusIn(planExecutionId, parentIds, statuses);
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
    EnumSet<Status> allowedStartStatuses = Status.obtainAllowedStartSet(status);
    Query query = query(where(NodeExecutionKeys.uuid).is(nodeExecutionId))
                      .addCriteria(where(NodeExecutionKeys.status).in(allowedStartStatuses));
    Update updateOps = new Update().set(NodeExecutionKeys.status, status);

    if (ops != null) {
      ops.accept(updateOps);
    }
    NodeExecution updated = mongoTemplate.findAndModify(query, updateOps, returnNewOptions, NodeExecution.class);
    if (updated == null) {
      logger.warn("Cannot update execution status for the node {} with {}", nodeExecutionId, status);
    }
    return updated;
  }
}
