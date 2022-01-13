/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;

import io.harness.eraro.Level;

public class K8sPodSyncException extends WingsException {
  public K8sPodSyncException(String message) {
    super(message, null, DEFAULT_ERROR_CODE, Level.ERROR, null, null);
  }

  public K8sPodSyncException(String message, Throwable cause) {
    super(message, cause, DEFAULT_ERROR_CODE, Level.ERROR, null, null);
  }
}
