package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.Status.IGNORE_FAILED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.Interrupt;

import javax.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class IgnoreFailedInterruptHandler extends MarkStatusInterruptHandler {
  @Override
  public Interrupt handleInterruptForNodeExecution(@NonNull @Valid Interrupt interrupt, String nodeExecutionId) {
    return super.handleInterruptStatus(interrupt, nodeExecutionId, IGNORE_FAILED);
  }
}
