package io.harness.engine.executions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.engine.interrupts.InterruptPackage.InterruptPackageBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.pms.contracts.advisers.InterventionWaitAdvise;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutInstance;

import com.google.inject.Inject;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@TypeAlias("interventionWaitTimeoutCallback")
public class InterventionWaitTimeoutCallback implements TimeoutCallback {
  @Inject @Transient private transient NodeExecutionService nodeExecutionService;
  @Inject @Transient private transient InterruptManager interruptManager;

  private final String planExecutionId;
  private final String nodeExecutionId;

  public InterventionWaitTimeoutCallback(String planExecutionId, String nodeExecutionId) {
    this.planExecutionId = planExecutionId;
    this.nodeExecutionId = nodeExecutionId;
  }

  @Override
  public void onTimeout(TimeoutInstance timeoutInstance) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution == null || !StatusUtils.finalizableStatuses().contains(nodeExecution.getStatus())) {
      return;
    }
    InterventionWaitAdvise interventionWaitAdvise = nodeExecution.getAdviserResponse().getInterventionWaitAdvise();
    interruptManager.register(getInterruptPackage(interventionWaitAdvise));
  }

  private InterruptPackage getInterruptPackage(InterventionWaitAdvise interventionWaitAdvise) {
    RepairActionCode repairActionCode = interventionWaitAdvise.getRepairActionCode();
    InterruptPackageBuilder interruptPackageBuilder =
        InterruptPackage.builder().planExecutionId(planExecutionId).nodeExecutionId(nodeExecutionId);
    switch (repairActionCode) {
      case MARK_AS_SUCCESS:
        return interruptPackageBuilder.interruptType(ExecutionInterruptType.MARK_SUCCESS).build();
      case RETRY:
        return interruptPackageBuilder.interruptType(ExecutionInterruptType.RETRY).build();
      case IGNORE:
      case ON_FAIL:
        return interruptPackageBuilder.interruptType(ExecutionInterruptType.NEXT_STEP).build();
      case END_EXECUTION:
        return interruptPackageBuilder.interruptType(ExecutionInterruptType.ABORT_ALL).build();
      default:
        throw new InvalidRequestException("No Execution Type Available for RepairAction Code: " + repairActionCode);
    }
  }
}
