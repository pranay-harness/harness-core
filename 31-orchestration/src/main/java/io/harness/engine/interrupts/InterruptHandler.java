package io.harness.engine.interrupts;

import io.harness.interrupts.Interrupt;

public interface InterruptHandler { Interrupt handleInterrupt(Interrupt interrupt); }
