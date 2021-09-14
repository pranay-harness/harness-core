/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.exception;

public class AnalysisStateMachineException extends RuntimeException {
  public AnalysisStateMachineException(Exception e) {
    super(e);
  }

  public AnalysisStateMachineException(String message) {
    super(message);
  }

  public AnalysisStateMachineException(String message, Exception e) {
    super(message, e);
  }
}
