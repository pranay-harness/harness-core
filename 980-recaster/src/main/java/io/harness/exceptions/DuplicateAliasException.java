/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exceptions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class DuplicateAliasException extends RuntimeException {
  public DuplicateAliasException(final String message) {
    super(message);
  }
  public DuplicateAliasException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
