/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.ENGINE_FUNCTOR_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.PIPELINE)
public class EngineFunctorException extends WingsException {
  private static final String REASON_ARG = "reason";

  public EngineFunctorException(String reason) {
    super(reason, null, ENGINE_FUNCTOR_ERROR, Level.ERROR, null, null);
    super.param(REASON_ARG, StringUtils.isBlank(reason) ? "Unknown reason" : reason);
  }

  public EngineFunctorException(String reason, Throwable throwable) {
    super(reason, throwable, ENGINE_FUNCTOR_ERROR, Level.ERROR, null, null);
    super.param(REASON_ARG, StringUtils.isBlank(reason) ? "Unknown reason" : reason);
  }

  public String getReason() {
    return (String) getParams().get(REASON_ARG);
  }
}
