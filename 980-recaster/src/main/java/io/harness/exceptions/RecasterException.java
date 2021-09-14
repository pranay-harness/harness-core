/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exceptions;

public class RecasterException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public RecasterException(final String message) {
    super(message);
  }

  public RecasterException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
