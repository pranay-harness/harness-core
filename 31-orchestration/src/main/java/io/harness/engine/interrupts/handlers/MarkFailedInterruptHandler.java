package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.execution.Status.FAILED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.Interrupt;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;

@OwnedBy(CDC)
@Slf4j
public class MarkFailedInterruptHandler extends MarkStatusInterruptHandler {
  @Override
  public Interrupt handleInterrupt(@NonNull @Valid Interrupt interrupt) {
    return super.handleInterruptStatus(interrupt, FAILED);
  }
}
