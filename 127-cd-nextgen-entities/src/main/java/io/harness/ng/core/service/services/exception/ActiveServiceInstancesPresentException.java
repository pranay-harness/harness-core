/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.service.services.exception;

import static io.harness.eraro.ErrorCode.ACTIVE_SERVICE_INSTANCES_PRESENT_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class ActiveServiceInstancesPresentException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ActiveServiceInstancesPresentException(String message) {
    super(message, null, ACTIVE_SERVICE_INSTANCES_PRESENT_EXCEPTION, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }
}