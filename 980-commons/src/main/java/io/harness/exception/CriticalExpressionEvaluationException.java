package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPRESSION_EVALUATION_FAILED;

import io.harness.eraro.Level;

public class CriticalExpressionEvaluationException extends WingsException {
  public static final String EXPRESSION_ARG = "expression";
  private static final String REASON_ARG = "reason";
  public CriticalExpressionEvaluationException(String reason, String expression) {
    super(null, null, EXPRESSION_EVALUATION_FAILED, Level.ERROR, null, null);
    super.param(REASON_ARG, reason);
    super.param(EXPRESSION_ARG, expression);
  }

  public CriticalExpressionEvaluationException(String reason, String expression, Throwable cause) {
    super(null, cause, EXPRESSION_EVALUATION_FAILED, Level.ERROR, null, null);
    super.param(REASON_ARG, reason);
    super.param(EXPRESSION_ARG, expression);
  }

  public CriticalExpressionEvaluationException(String reason) {
    super(null, null, EXPRESSION_EVALUATION_FAILED, Level.ERROR, null, null);
    super.param(REASON_ARG, reason);
  }
}
