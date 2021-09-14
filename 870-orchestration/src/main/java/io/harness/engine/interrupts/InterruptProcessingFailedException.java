/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.pms.contracts.interrupts.InterruptType;

@OwnedBy(CDC)
public class InterruptProcessingFailedException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public InterruptProcessingFailedException(InterruptType interruptType, String message) {
    super(null, null, ErrorCode.ENGINE_INTERRUPT_PROCESSING_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, HarnessStringUtils.join("", "[Interrupt Type: ", interruptType.toString(), "]", message));
  }

  public InterruptProcessingFailedException(InterruptType interruptType, String message, Throwable cause) {
    super(message, cause, ErrorCode.ENGINE_INTERRUPT_PROCESSING_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, HarnessStringUtils.join("", "[Interrupt Type: ", interruptType.toString(), "]", message));
  }
}
