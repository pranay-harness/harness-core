package io.harness.engine.status;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.status.Status.PAUSED;
import static io.harness.execution.status.Status.flowingStatuses;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.interrupts.InterruptEffect;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery;

import java.util.Date;
import java.util.List;

public class PausedStepStatusUpdate implements StepStatusUpdate {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    boolean pausePlan = pauseParents(stepStatusUpdateInfo.getNodeExecutionId(), stepStatusUpdateInfo.getInterruptId());
    if (pausePlan) {
      planExecutionService.update(
          stepStatusUpdateInfo.getPlanExecutionId(), ops -> ops.set(PlanExecutionKeys.status, PAUSED));
    }
  }

  private boolean pauseParents(String nodeExecutionId, String interruptId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution.getParentId() == null) {
      return true;
    }
    List<NodeExecution> flowingChildren = hPersistence.createQuery(NodeExecution.class, HQuery.excludeAuthority)
                                              .filter(NodeExecutionKeys.parentId, nodeExecution.getParentId())
                                              .field(NodeExecutionKeys.status)
                                              .in(flowingStatuses())
                                              .project(NodeExecutionKeys.uuid, true)
                                              .asList();
    if (isEmpty(flowingChildren)) {
      nodeExecutionService.update(nodeExecution.getParentId(),
          ops
          -> ops.set(NodeExecutionKeys.status, PAUSED)
                 .addToSet(NodeExecutionKeys.interruptHistories,
                     InterruptEffect.builder().interruptId(interruptId).tookEffectAt(new Date())));
      return pauseParents(nodeExecution.getParentId(), interruptId);
    } else {
      return false;
    }
  }
}
