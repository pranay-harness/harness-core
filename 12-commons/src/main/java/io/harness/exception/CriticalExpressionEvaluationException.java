package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPRESSION_EVALUATION_FAILED;

public class CriticalExpressionEvaluationException extends WingsException {
  private static final String EXPRESSION_KEY = "expression";
  private static final String REASON_KEY = "reason";
  public CriticalExpressionEvaluationException(String reason, String expression) {
    super(EXPRESSION_EVALUATION_FAILED);
    super.addParam(REASON_KEY, reason);
    super.addParam(EXPRESSION_KEY, expression);
  }
}
