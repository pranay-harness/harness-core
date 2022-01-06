/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class InvalidRollbackException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public InvalidRollbackException(String details, ErrorCode errorCode) {
    super(null, null, errorCode, Level.ERROR, null, null);
    super.param(DETAILS_KEY, details);
  }
}
