/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.CONNECTOR_NOT_FOUND_EXCEPTION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.DX)
public class ConnectorNotFoundException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public ConnectorNotFoundException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, CONNECTOR_NOT_FOUND_EXCEPTION, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }
}
