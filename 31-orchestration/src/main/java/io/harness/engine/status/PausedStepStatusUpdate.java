package io.harness.engine.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.status.Status.PAUSED;
import static io.harness.execution.status.Status.flowingStatuses;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.InterruptEffect;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@OwnedBy(CDC)
public class PausedStepStatusUpdate implements StepStatusUpdate {
  @Inject private MongoTemplate mongoTemplate;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    boolean pausePlan = pauseParents(stepStatusUpdateInfo.getNodeExecutionId(), stepStatusUpdateInfo.getInterruptId());
    if (pausePlan) {
      planExecutionService.updateStatus(stepStatusUpdateInfo.getPlanExecutionId(), PAUSED);
    }
  }

  private boolean pauseParents(String nodeExecutionId, String interruptId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution.getParentId() == null) {
      return true;
    }
    Query query = query(where(NodeExecutionKeys.parentId).is(nodeExecution.getParentId()))
                      .addCriteria(where(NodeExecutionKeys.status).in(flowingStatuses()));
    query.fields().include(NodeExecutionKeys.uuid);
    List<NodeExecution> flowingChildren = mongoTemplate.find(query, NodeExecution.class);
    if (isEmpty(flowingChildren)) {
      // Update Status
      nodeExecutionService.updateStatusWithOps(nodeExecution.getParentId(), PAUSED,
          ops
          -> ops.addToSet(NodeExecutionKeys.interruptHistories,
              InterruptEffect.builder()
                  .interruptId(interruptId)
                  .tookEffectAt(System.currentTimeMillis())
                  .interruptType(ExecutionInterruptType.PAUSE_ALL)
                  .build()));
      return pauseParents(nodeExecution.getParentId(), interruptId);
    } else {
      return false;
    }
  }
}
