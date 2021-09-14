/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.globalcontex;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@TypeAlias("ErrorHandlingGlobalContextData")
public class ErrorHandlingGlobalContextData implements GlobalContextData {
  public static final String IS_SUPPORTED_ERROR_FRAMEWORK = "IS_SUPPORTED_ERROR_FRAMEWORK";
  boolean isSupportedErrorFramework;

  @Override
  public String getKey() {
    return IS_SUPPORTED_ERROR_FRAMEWORK;
  }
}
