/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.distribution.idempotence;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

public class UnableToRegisterIdempotentOperationException extends WingsException {
  public UnableToRegisterIdempotentOperationException(String message) {
    super(ErrorCode.UNKNOWN_ERROR, message);
  }

  public UnableToRegisterIdempotentOperationException(Exception cause) {
    super(ErrorCode.UNKNOWN_ERROR, cause);
  }
}
