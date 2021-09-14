/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.core.execution.events.node.resume;

import io.harness.exception.FailureType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.tasks.ErrorResponseData;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DummyErrorResponseData implements ErrorResponseData {
  @Override
  public String getErrorMessage() {
    return "error";
  }

  @Override
  public EnumSet<FailureType> getFailureTypes() {
    return EnumSet.of(FailureType.CONNECTIVITY);
  }

  @Override
  public WingsException getException() {
    return new InvalidRequestException("Dummy Invalid Request Exception");
  }
}
