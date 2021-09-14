/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@Value
@OwnedBy(HarnessTeam.PIPELINE)
public class JexlRuntimeException extends RuntimeException {
  String expression;
  Throwable rootCause;

  public JexlRuntimeException(String expression, Throwable rootCause) {
    this.expression = expression;
    this.rootCause = rootCause;
  }
}
