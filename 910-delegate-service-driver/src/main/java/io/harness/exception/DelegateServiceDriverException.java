/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceDriverException extends RuntimeException {
  public DelegateServiceDriverException(String message, Throwable cause) {
    super(message, cause);
  }

  public DelegateServiceDriverException(String message) {
    super(message);
  }
}
