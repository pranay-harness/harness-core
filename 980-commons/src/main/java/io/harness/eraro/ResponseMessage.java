/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eraro;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.Level.ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.FailureType;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class ResponseMessage {
  @Default ErrorCode code = DEFAULT_ERROR_CODE;
  @Default Level level = ERROR;

  String message;
  Throwable exception;
  @Default EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
}
