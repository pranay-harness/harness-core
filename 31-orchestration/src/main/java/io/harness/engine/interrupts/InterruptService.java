package io.harness.engine.interrupts;

import io.harness.ambiance.Ambiance;
import io.harness.interrupts.Interrupt;
import io.harness.state.io.StepTransput;

import java.util.List;

public interface InterruptService {
  List<Interrupt> fetchActiveInterrupts(String planExecutionId);

  Interrupt seize(String interruptId);

  List<Interrupt> fetchActivePlanLevelInterrupts(String planExecutionId);

  InterruptCheck checkAndHandleInterruptsBeforeNodeStart(Ambiance ambiance, List<StepTransput> additionalInputs);
}
