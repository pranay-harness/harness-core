/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.exceptions;

public class ServiceGuardAnalysisException extends RuntimeException {
  public ServiceGuardAnalysisException(Exception e) {
    super(e);
  }

  public ServiceGuardAnalysisException(String message) {
    super(message);
  }

  public ServiceGuardAnalysisException(String message, Exception e) {
    super(message, e);
  }
}
