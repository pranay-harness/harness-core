package io.harness.engine.advise.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserIssuer;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.InterruptConfig;
import io.harness.pms.contracts.advisers.IssuedBy;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public class MarkSuccessAdviseHandler implements AdviserResponseHandler {
  @Inject private InterruptManager interruptManager;

  @Override
  public void handleAdvise(NodeExecution nodeExecution, AdviserResponse adviserResponse) {
    InterruptPackage interruptPackage =
        InterruptPackage.builder()
            .planExecutionId(nodeExecution.getAmbiance().getPlanExecutionId())
            .nodeExecutionId(nodeExecution.getUuid())
            .interruptType(InterruptType.MARK_SUCCESS)
            .interruptConfig(
                InterruptConfig.newBuilder()
                    .setIssuedBy(IssuedBy.newBuilder()
                                     .setAdviserIssuer(
                                         AdviserIssuer.newBuilder().setAdviserType(AdviseType.MARK_SUCCESS).build())
                                     .build())
                    .build())
            .build();
    interruptManager.register(interruptPackage);
  }
}
