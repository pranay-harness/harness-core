/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import org.apache.commons.lang3.StringUtils;

public class FunctorException extends CriticalExpressionEvaluationException {
  public FunctorException(String reason) {
    super(reason, StringUtils.EMPTY);
  }

  public FunctorException(String reason, Throwable cause) {
    super(reason, StringUtils.EMPTY, cause);
  }
}
