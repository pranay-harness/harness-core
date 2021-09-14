/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exceptions;

public class CastedFieldException extends RuntimeException {
  public CastedFieldException(final String message) {
    super(message);
  }

  public CastedFieldException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
