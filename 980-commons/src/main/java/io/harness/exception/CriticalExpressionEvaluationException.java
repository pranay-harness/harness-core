/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPRESSION_EVALUATION_FAILED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(HarnessTeam.PIPELINE)
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

  public String getReason() {
    return (String) getParams().get(REASON_ARG);
  }

  public String getExpression() {
    return (String) getParams().get(EXPRESSION_ARG);
  }
}
