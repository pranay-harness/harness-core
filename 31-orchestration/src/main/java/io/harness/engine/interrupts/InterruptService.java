package io.harness.engine.interrupts;

import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.State;

import java.util.List;
import javax.validation.Valid;

public interface InterruptService {
  List<Interrupt> fetchActiveInterrupts(String planExecutionId);

  List<Interrupt> fetchAllInterrupts(String planExecutionId);

  Interrupt markProcessed(String interruptId, State interruptState);

  Interrupt markProcessing(String interruptId);

  List<Interrupt> fetchActivePlanLevelInterrupts(String planExecutionId);

  InterruptCheck checkAndHandleInterruptsBeforeNodeStart(String planExecutionId, String nodeExecutionId);

  Interrupt save(@Valid Interrupt interrupt);

  Interrupt get(String interruptId);
}
