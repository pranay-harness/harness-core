/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
