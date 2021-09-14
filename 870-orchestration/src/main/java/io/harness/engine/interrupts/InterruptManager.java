/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.Interrupt;
import io.harness.logging.AutoLogContext;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class InterruptManager {
  @Inject private InterruptHandlerFactory interruptHandlerFactory;

  public Interrupt register(InterruptPackage interruptPackage) {
    Interrupt interrupt = Interrupt.builder()
                              .uuid(generateUuid())
                              .planExecutionId(interruptPackage.getPlanExecutionId())
                              .type(interruptPackage.getInterruptType())
                              .metadata(interruptPackage.getMetadata())
                              .nodeExecutionId(interruptPackage.getNodeExecutionId())
                              .parameters(interruptPackage.getParameters())
                              .interruptConfig(interruptPackage.getInterruptConfig())
                              .build();
    try (AutoLogContext ignore = interrupt.autoLogContext()) {
      InterruptHandler interruptHandler = interruptHandlerFactory.obtainHandler(interruptPackage.getInterruptType());
      return interruptHandler.registerInterrupt(interrupt);
    }
  }
}
