package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.Status.PAUSED;
import static io.harness.pms.contracts.execution.Status.RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.NodeStatusUpdateHandler;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;
import java.util.EnumSet;

@OwnedBy(PIPELINE)
public class ResumeStepStatusUpdate implements NodeStatusUpdateHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void handleNodeStatusUpdate(NodeUpdateInfo nodeStatusUpdateInfo) {
    boolean resumePlan = resumeParents(nodeStatusUpdateInfo.getNodeExecution());
    if (resumePlan) {
      Status planStatus = planExecutionService.calculateStatus(nodeStatusUpdateInfo.getPlanExecutionId());
      if (!StatusUtils.isFinalStatus(planStatus)) {
        planExecutionService.updateStatus(nodeStatusUpdateInfo.getPlanExecutionId(), planStatus);
      }
    }
  }

  private boolean resumeParents(NodeExecution nodeExecution) {
    if (nodeExecution.getParentId() == null) {
      return true;
    }
    NodeExecution parentNodeExecution =
        nodeExecutionService.updateStatusWithOps(nodeExecution.getParentId(), RUNNING, null, EnumSet.of(PAUSED));
    return parentNodeExecution != null && resumeParents(parentNodeExecution);
  }
}