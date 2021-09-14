/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.exception;

public class AnalysisOrchestrationException extends RuntimeException {
  public AnalysisOrchestrationException(Exception e) {
    super(e);
  }

  public AnalysisOrchestrationException(String message) {
    super(message);
  }

  public AnalysisOrchestrationException(String message, Exception e) {
    super(message, e);
  }
}
