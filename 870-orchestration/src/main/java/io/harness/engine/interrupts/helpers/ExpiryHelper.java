package io.harness.engine.interrupts.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.logging.UnitStatus.EXPIRED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionUpdateFailedException;
import io.harness.engine.interrupts.InterruptProcessingFailedException;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.interrupts.Interrupt;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
public class ExpiryHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject private InterruptHelper interruptHelper;

  public String expireMarkedInstance(NodeExecution nodeExecution, Interrupt interrupt) {
    try {
      boolean taskDiscontinued = interruptHelper.discontinueTaskIfRequired(nodeExecution);
      if (!taskDiscontinued) {
        log.error("Delegate Task Cannot be aborted for NodeExecutionId: {}", nodeExecution.getUuid());
      }

      List<UnitProgress> unitProgressList = new ArrayList<>();
      if (!EmptyPredicate.isEmpty(nodeExecution.getUnitProgresses())) {
        for (UnitProgress up : nodeExecution.getUnitProgresses()) {
          if (isFinalUnitProgress(up.getStatus())) {
            unitProgressList.add(up);
          } else {
            unitProgressList.add(up.toBuilder().setStatus(EXPIRED).setEndTime(System.currentTimeMillis()).build());
          }
        }
      }

      NodeExecution updatedNodeExecution =
          nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), Status.EXPIRED,
              ops
              -> ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis())
                     .set(NodeExecutionKeys.unitProgresses, unitProgressList));
      engine.endTransition(updatedNodeExecution, null);
      return interrupt.getUuid();
    } catch (NodeExecutionUpdateFailedException ex) {
      throw new InterruptProcessingFailedException(
          InterruptType.MARK_EXPIRED, "Expiry failed failed for NodeExecutionId: " + nodeExecution.getUuid(), ex);
    } catch (Exception e) {
      log.error("Error in discontinuing", e);
      throw new InvalidRequestException("Error in discontinuing, " + e.getMessage());
    }
  }

  private boolean isFinalUnitProgress(UnitStatus status) {
    return EnumSet.of(UnitStatus.FAILURE, UnitStatus.SKIPPED, UnitStatus.SUCCESS).contains(status);
  }
}
