/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.barrier;

public class UnableToLoadBarrierException extends Exception {
  public UnableToLoadBarrierException(String message) {
    super(message);
  }

  public UnableToLoadBarrierException(Exception cause) {
    super(cause);
  }
}
