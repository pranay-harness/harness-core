package io.harness.engine.interrupts;

import io.harness.interrupts.Interrupt;

import java.util.List;

public interface InterruptService {
  List<Interrupt> fetchActiveInterrupts(String planExecutionId);

  boolean seize(String interruptId);
}
