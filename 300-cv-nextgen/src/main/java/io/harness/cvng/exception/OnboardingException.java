/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.exception;

public class OnboardingException extends RuntimeException {
  public OnboardingException(Exception e) {
    super(e);
  }

  public OnboardingException(String message) {
    super(message);
  }

  public OnboardingException(String message, Exception e) {
    super(message, e);
  }
}
