/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.tasks;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.FailureType;
import io.harness.exception.WingsException;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public class FailureResponseData implements ErrorResponseData {
  String errorMessage;
  EnumSet<FailureType> failureTypes;
  WingsException exception;
  // TODO : Add more fields here for better handling
}
