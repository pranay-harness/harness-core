/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public class AuthenticationRuntimeException extends AuthenticationAndAuthorizationRuntimeException {
  public AuthenticationRuntimeException(String message) {
    super(message);
  }

  public AuthenticationRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
