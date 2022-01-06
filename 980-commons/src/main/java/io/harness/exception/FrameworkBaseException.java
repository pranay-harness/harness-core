/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.Level.INFO;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;

/**
 * This exception serves as base class for all dedicated exceptions added to support
 * error handling framework
 */
@OwnedBy(HarnessTeam.DX)
public abstract class FrameworkBaseException extends WingsException {
  public FrameworkBaseException(Throwable cause, ErrorCode errorCode) {
    super(null, cause, errorCode, INFO, null, null);
  }
}
