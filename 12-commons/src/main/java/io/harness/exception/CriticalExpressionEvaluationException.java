package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPRESSION_EVALUATION_FAILED;

public class CriticalExpressionEvaluationException extends WingsException {
  public static final String EXPRESSION_ARG = "expression";
  private static final String REASON_ARG = "reason";
  public CriticalExpressionEvaluationException(String reason, String expression) {
    super(EXPRESSION_EVALUATION_FAILED);
    super.addParam(REASON_ARG, reason);
    super.addParam(EXPRESSION_ARG, expression);
  }

  public CriticalExpressionEvaluationException(String reason, String expression, Throwable cause) {
    super(EXPRESSION_EVALUATION_FAILED, cause);
    super.addParam(REASON_ARG, reason);
    super.addParam(EXPRESSION_ARG, expression);
  }

  public CriticalExpressionEvaluationException(String reason) {
    super(EXPRESSION_EVALUATION_FAILED);
    super.addParam(REASON_ARG, reason);
  }
}