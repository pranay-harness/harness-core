/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.interrupts;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.Interrupt;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class InterruptUtils {
  public Optional<Interrupt> obtainOptionalInterruptFromActiveInterrupts(
      List<Interrupt> interrupts, String planExecutionId, String nodeExecutionId) {
    Optional<Interrupt> optionalInterrupt;
    if (interrupts.stream().anyMatch(interrupt
            -> interrupt.getNodeExecutionId() != null && interrupt.getNodeExecutionId().equals(nodeExecutionId))) {
      optionalInterrupt = interrupts.stream()
                              .filter(interrupt
                                  -> interrupt.getPlanExecutionId().equals(planExecutionId)
                                      && interrupt.getNodeExecutionId().equals(nodeExecutionId))
                              .findFirst();
    } else {
      optionalInterrupt =
          interrupts.stream().filter(interrupt -> interrupt.getPlanExecutionId().equals(planExecutionId)).findFirst();
    }

    return optionalInterrupt;
  }

  public Optional<Interrupt> obtainOptionalInterruptFromActiveInterruptsWithPredicates(List<Interrupt> interrupts,
      String planExecutionId, String nodeExecutionId, List<Predicate<Interrupt>> predicates) {
    Predicate<Interrupt> predicate = predicates.stream().reduce(t -> true, Predicate::and);
    Optional<Interrupt> optionalInterrupt;
    if (interrupts.stream().anyMatch(interrupt
            -> interrupt.getNodeExecutionId() != null && interrupt.getNodeExecutionId().equals(nodeExecutionId))) {
      optionalInterrupt =
          interrupts.stream()
              .filter(interrupt
                  -> interrupt.getPlanExecutionId().equals(planExecutionId)
                      && interrupt.getNodeExecutionId().equals(nodeExecutionId) && predicate.test(interrupt))
              .findFirst();
    } else {
      optionalInterrupt =
          interrupts.stream()
              .filter(interrupt -> interrupt.getPlanExecutionId().equals(planExecutionId) && predicate.test(interrupt))
              .findFirst();
    }

    return optionalInterrupt;
  }
}
