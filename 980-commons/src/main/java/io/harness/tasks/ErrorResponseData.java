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

@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public interface ErrorResponseData extends ResponseData {
  String getErrorMessage();
  EnumSet<FailureType> getFailureTypes();
  WingsException getException();
}
