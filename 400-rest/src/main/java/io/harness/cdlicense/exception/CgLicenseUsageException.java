/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdlicense.exception;

import static io.harness.eraro.ErrorCode.CG_LICENSE_USAGE_ERROR;
import static io.harness.eraro.ErrorCode.DEPLOYMENT_MIGRATION_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class CgLicenseUsageException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public CgLicenseUsageException(String message, Throwable cause) {
    super(message, cause, CG_LICENSE_USAGE_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
