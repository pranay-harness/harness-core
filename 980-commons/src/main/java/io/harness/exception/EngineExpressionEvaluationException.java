/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.ENGINE_EXPRESSION_EVALUATION_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import org.apache.commons.lang3.StringUtils;

// NOTE: This is for internal use by the expression engine.
@OwnedBy(HarnessTeam.PIPELINE)
public class EngineExpressionEvaluationException extends WingsException {
  private static final String EXPRESSION_ARG = "expression";
  private static final String REASON_ARG = "reason";

  public EngineExpressionEvaluationException(String reason, String expression) {
    super(reason, null, ENGINE_EXPRESSION_EVALUATION_ERROR, Level.ERROR, null, null);
    super.param(EXPRESSION_ARG, StringUtils.isBlank(expression) ? "unknown" : expression);
    super.param(REASON_ARG, StringUtils.isBlank(reason) ? "Unknown reason" : reason);
  }

  public EngineExpressionEvaluationException(EngineFunctorException functorException, String expression) {
    this(functorException.getReason(), expression);
  }

  public EngineExpressionEvaluationException(FunctorException functorException, String expression) {
    this(functorException.getReason(), expression);
  }
}
