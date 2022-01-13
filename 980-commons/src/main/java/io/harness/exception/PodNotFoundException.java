/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.POD_NOT_FOUND_ERROR;

import io.harness.eraro.Level;

public class PodNotFoundException extends WingsException {
  private static final String REASON_ARG = "reason";

  public PodNotFoundException(String reason) {
    this(reason, null);
  }

  public PodNotFoundException(String reason, Throwable cause) {
    super(null, cause, POD_NOT_FOUND_ERROR, Level.ERROR, null, null);
    super.param(REASON_ARG, reason);
  }
}
