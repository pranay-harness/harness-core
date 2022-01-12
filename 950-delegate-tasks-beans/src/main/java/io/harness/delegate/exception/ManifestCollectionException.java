/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.exception;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;

import java.util.EnumSet;

public class ManifestCollectionException extends WingsException {
  public ManifestCollectionException(String message) {
    super(message, null, GENERAL_ERROR, Level.ERROR, null, null);
  }

  public ManifestCollectionException(String message, Throwable cause) {
    super(message, cause, GENERAL_ERROR, Level.ERROR, null, null);
  }

  public ManifestCollectionException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, GENERAL_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }

  public ManifestCollectionException(String message, EnumSet<ReportTarget> reportTargets, Throwable t) {
    super(message, t, GENERAL_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }
}
