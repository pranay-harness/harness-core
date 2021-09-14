/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.barrier;

public class UnableToSaveBarrierException extends Exception {
  public UnableToSaveBarrierException(String message) {
    super(message);
  }

  public UnableToSaveBarrierException(Exception cause) {
    super(cause);
  }
}
