/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.health;

import static io.harness.eraro.ErrorCode.HEALTH_ERROR;

import io.harness.exception.WingsException;

public class HealthException extends WingsException {
  private static final String REASON_KEY = "reason";

  public HealthException(String reason) {
    super(HEALTH_ERROR);
    addParam(REASON_KEY, reason);
  }

  public HealthException(String reason, Throwable cause) {
    super(HEALTH_ERROR, cause);
    addParam(REASON_KEY, reason);
  }
}
