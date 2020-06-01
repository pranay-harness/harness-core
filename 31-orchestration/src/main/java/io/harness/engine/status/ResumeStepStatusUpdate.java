package io.harness.engine.status;

import static io.harness.execution.status.Status.PAUSED;
import static io.harness.execution.status.Status.RUNNING;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.interrupts.InterruptEffect;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;

import java.util.Date;
import java.util.List;

public class ResumeStepStatusUpdate implements StepStatusUpdate {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    Query<NodeExecution> query =
        hPersistence.createQuery(NodeExecution.class, excludeAuthority)
            .filter(NodeExecutionKeys.planExecutionId, stepStatusUpdateInfo.getPlanExecutionId())
            .filter(NodeExecutionKeys.status, PAUSED)
            .filter(NodeExecutionKeys.parentId, null);
    query.criteria(NodeExecutionKeys.parentId).doesNotExist().criteria(NodeExecutionKeys.parentId).equal(null);
    List<NodeExecution> nodeExecutions = query.asList();

    for (NodeExecution nodeExecution : nodeExecutions) {
      resumeParents(nodeExecution.getUuid(), stepStatusUpdateInfo.getInterruptId());
    }

    planExecutionService.update(
        stepStatusUpdateInfo.getPlanExecutionId(), ops -> ops.set(PlanExecutionKeys.status, RUNNING));
  }

  private void resumeParents(String nodeExecutionId, String interruptId) {
    NodeExecution nodeExecution = nodeExecutionService.update(nodeExecutionId,
        ops
        -> ops.set(NodeExecutionKeys.status, RUNNING)
               .addToSet(NodeExecutionKeys.interruptHistories,
                   InterruptEffect.builder().interruptId(interruptId).tookEffectAt(new Date()).build()));
    if (nodeExecution.getParentId() == null) {
      return;
    }
    resumeParents(nodeExecution.getParentId(), interruptId);
  }
}
