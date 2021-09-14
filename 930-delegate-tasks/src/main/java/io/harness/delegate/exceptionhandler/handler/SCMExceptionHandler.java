/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.exceptionhandler.handler;

import static io.harness.exception.WingsException.USER;

import io.harness.eraro.ErrorCode;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.SCMRuntimeException;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class SCMExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(SCMRuntimeException.class).build();
  }
  @Override
  public WingsException handleException(Exception exception) {
    SCMRuntimeException scmException = (SCMRuntimeException) exception;
    ErrorCode errorCode = scmException.getErrorCode();

    if (errorCode == ErrorCode.SCM_UNAUTHORIZED) {
      return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_INVALID_GIT_API_AUTHORIZATION,
          ExplanationException.INVALID_GIT_API_AUTHORIZATION,
          new InvalidRequestException(exception.getMessage(), USER));
    }
    return new InvalidRequestException(exception.getMessage(), USER);
  }
}
