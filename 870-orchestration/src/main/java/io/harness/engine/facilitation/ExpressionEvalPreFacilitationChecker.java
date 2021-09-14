/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.facilitation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionCheck;
import io.harness.engine.OrchestrationEngine;
import io.harness.exception.runtime.JexlRuntimeException;
import io.harness.pms.contracts.ambiance.Ambiance;

import com.google.inject.Inject;
import org.apache.commons.jexl3.JexlException;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class ExpressionEvalPreFacilitationChecker extends AbstractPreFacilitationChecker {
  @Inject private OrchestrationEngine orchestrationEngine;

  ExecutionCheck handleExpressionEvaluationError(Exception ex, String conditionExpression, Ambiance ambiance) {
    Exception cascadedException = ex;
    if (ex instanceof JexlException) {
      cascadedException = new JexlRuntimeException(conditionExpression, ex);
    }
    orchestrationEngine.handleError(ambiance, cascadedException);
    return ExecutionCheck.builder()
        .proceed(false)
        .reason("Error in evaluating configured when condition on step")
        .build();
  }
}
