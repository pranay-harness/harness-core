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