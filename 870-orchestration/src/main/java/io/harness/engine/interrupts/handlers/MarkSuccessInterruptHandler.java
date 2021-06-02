package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.Status.SUCCEEDED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.execution.Status;

import java.util.EnumSet;
import javax.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class MarkSuccessInterruptHandler extends MarkStatusInterruptHandler {
  @Override
  public Interrupt handleInterrupt(@NonNull @Valid Interrupt interrupt) {
    return super.handleInterruptStatus(
        interrupt, SUCCEEDED, EnumSet.of(Status.FAILED, Status.EXPIRED, Status.ERRORED, Status.INTERVENTION_WAITING));
  }
}
