/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class UnauthorizedUsageRestrictionsException extends WingsException {
  public UnauthorizedUsageRestrictionsException(EnumSet<ReportTarget> reportTarget) {
    super(null, null, USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, Level.ERROR, reportTarget, null);
  }
}
