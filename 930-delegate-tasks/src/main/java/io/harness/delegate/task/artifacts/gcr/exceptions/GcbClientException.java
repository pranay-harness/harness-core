/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts.gcr.exceptions;

public class GcbClientException extends RuntimeException {
  public GcbClientException(String message, Throwable cause) {
    super(message, cause);
  }
  public GcbClientException(String message) {
    super(message);
  }
}
