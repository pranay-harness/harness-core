/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;

import io.harness.eraro.Level;

import java.time.Duration;

public class PollTimeoutException extends WingsException {
  public PollTimeoutException(Duration timeout) {
    super("The condition was not met after " + timeout.toString(), null, UNKNOWN_ERROR, Level.ERROR, null, null);
  }
}
