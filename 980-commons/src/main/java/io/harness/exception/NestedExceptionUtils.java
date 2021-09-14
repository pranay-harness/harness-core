/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

public class NestedExceptionUtils {
  public static WingsException hintWithExplanationException(
      String hintMessage, String explanationMessage, Throwable cause) {
    return new HintException(hintMessage, new ExplanationException(explanationMessage, cause));
  }
}
