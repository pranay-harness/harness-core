/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.eraro.ErrorCode.MIGRATION_EXCEPTION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(DX)
public class MigrationException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public MigrationException(String message) {
    super(message, null, MIGRATION_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public MigrationException(String message, Throwable cause) {
    super(message, cause, MIGRATION_EXCEPTION, Level.ERROR, null, null);
  }
}
