package io.harness.engine.interrupts;

import io.harness.ambiance.Ambiance;
import io.harness.interrupts.Interrupt;
import io.harness.state.io.StepTransput;

import java.util.List;

public interface InterruptHandler {
  Interrupt registerInterrupt(Interrupt interrupt);

  Interrupt handleInterrupt(Interrupt interrupt, Ambiance ambiance, List<StepTransput> additionalInputs);
}
