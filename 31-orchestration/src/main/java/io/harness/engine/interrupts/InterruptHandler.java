package io.harness.engine.interrupts;

import io.harness.ambiance.Ambiance;
import io.harness.interrupts.Interrupt;

public interface InterruptHandler {
  Interrupt registerInterrupt(Interrupt interrupt);

  Interrupt handleInterrupt(Interrupt interrupt, Ambiance ambiance);
}
