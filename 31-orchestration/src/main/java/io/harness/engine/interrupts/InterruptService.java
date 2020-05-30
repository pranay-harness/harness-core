package io.harness.engine.interrupts;

import io.harness.ambiance.Ambiance;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.State;
import io.harness.state.io.StepTransput;

import java.util.List;

public interface InterruptService {
  List<Interrupt> fetchActiveInterrupts(String planExecutionId);

  List<Interrupt> fetchAllInterrupts(String planExecutionId);

  Interrupt markProcessed(String interruptId, State interruptState);

  Interrupt markProcessing(String interruptId);

  List<Interrupt> fetchActivePlanLevelInterrupts(String planExecutionId);

  InterruptCheck checkAndHandleInterruptsBeforeNodeStart(Ambiance ambiance, List<StepTransput> additionalInputs);
}
