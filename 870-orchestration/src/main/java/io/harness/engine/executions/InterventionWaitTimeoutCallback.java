package io.harness.engine.executions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.engine.interrupts.InterruptPackage.InterruptPackageBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserIssuer;
import io.harness.pms.contracts.advisers.InterruptConfig;
import io.harness.pms.contracts.advisers.InterventionWaitAdvise;
import io.harness.pms.contracts.advisers.IssuedBy;
import io.harness.pms.contracts.advisers.RetryInterruptConfig;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutDetails;
import io.harness.timeout.TimeoutInstance;

import com.google.common.annotations.VisibleForTesting;
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
    nodeExecutionService.update(
        nodeExecutionId, ops -> ops.set(NodeExecutionKeys.adviserTimeoutDetails, new TimeoutDetails(timeoutInstance)));
    interruptManager.register(getInterruptPackage(interventionWaitAdvise));
  }

  @VisibleForTesting
  InterruptPackage getInterruptPackage(InterventionWaitAdvise interventionWaitAdvise) {
    RepairActionCode repairActionCode = interventionWaitAdvise.getRepairActionCode();
    InterruptConfig.Builder interruptConfigBuilder = InterruptConfig.newBuilder().setIssuedBy(
        IssuedBy.newBuilder()
            .setAdviserIssuer(AdviserIssuer.newBuilder().setAdviserType(AdviseType.INTERVENTION_WAIT).build())
            .build());
    InterruptPackageBuilder interruptPackageBuilder =
        InterruptPackage.builder().planExecutionId(planExecutionId).nodeExecutionId(nodeExecutionId);
    switch (repairActionCode) {
      case MARK_AS_SUCCESS:
        return interruptPackageBuilder.interruptType(InterruptType.MARK_SUCCESS)
            .interruptConfig(interruptConfigBuilder.build())
            .build();
      case RETRY:
        interruptConfigBuilder.setRetryInterruptConfig(RetryInterruptConfig.newBuilder().build());
        return interruptPackageBuilder.interruptType(InterruptType.RETRY)
            .interruptConfig(interruptConfigBuilder.build())
            .build();
      case IGNORE:
        return interruptPackageBuilder.interruptType(InterruptType.IGNORE)
            .interruptConfig(interruptConfigBuilder.build())
            .build();
      case ON_FAIL:
        return interruptPackageBuilder.interruptType(InterruptType.MARK_FAILED)
            .interruptConfig(interruptConfigBuilder.build())
            .build();
      case STAGE_ROLLBACK:
      case STEP_GROUP_ROLLBACK:
      case CUSTOM_FAILURE:
        return interruptPackageBuilder.interruptType(InterruptType.CUSTOM_FAILURE)
            .interruptConfig(interruptConfigBuilder.build())
            .metadata(interventionWaitAdvise.getMetadataMap())
            .build();
      case UNKNOWN:
      case END_EXECUTION:
        return interruptPackageBuilder.interruptType(InterruptType.ABORT_ALL)
            .interruptConfig(interruptConfigBuilder.build())
            .build();
      default:
        throw new InvalidRequestException("No Execution Type Available for RepairAction Code: " + repairActionCode);
    }
  }
}
