/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.stackdriver;

public class StackdriverSetupException extends RuntimeException {
  public StackdriverSetupException(String errMsg) {
    super(errMsg);
  }

  public StackdriverSetupException(Throwable throwable) {
    super(throwable);
  }
}
