package io.harness.engine.facilitation;

import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.PreFacilitationCheck;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Inject;

public class InterruptPreFacilitationChecker extends AbstractPreFacilitationChecker {
  @Inject private InterruptService interruptService;

  @Override
  protected PreFacilitationCheck performCheck(NodeExecution nodeExecution) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    return interruptService.checkAndHandleInterruptsBeforeNodeStart(
        ambiance.getPlanExecutionId(), AmbianceUtils.obtainCurrentRuntimeId(ambiance));
  }
}
