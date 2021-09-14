/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception.exceptionmanager.exceptionhandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.EngineExpressionEvaluationException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.runtime.JexlRuntimeException;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class JexlRuntimeExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(JexlRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    if (exception instanceof JexlRuntimeException) {
      JexlRuntimeException jexlRuntimeException = (JexlRuntimeException) exception;
      String expression = jexlRuntimeException.getExpression();
      String message;
      if (jexlRuntimeException.getRootCause() != null) {
        message = ExceptionUtils.getMessage(jexlRuntimeException.getRootCause());
      } else {
        message = ExceptionUtils.getMessage(exception);
      }
      if (message.contains("parsing")) {
        message = "Expression could not be resolved due to incorrect expression format.";
      }
      return new EngineExpressionEvaluationException(message, expression);
    }
    return new InvalidRequestException(ExceptionUtils.getMessage(exception));
  }
}
