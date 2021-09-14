/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.springdata.exceptions;

import static io.harness.exception.FailureType.APPLICATION_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class WingsTransactionFailureException extends WingsException {
  private static final String MESSAGE_ARG = "exception_message";

  protected WingsTransactionFailureException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, null, Level.ERROR, reportTargets, EnumSet.of(APPLICATION_ERROR));
    super.param(MESSAGE_ARG, message);
  }
}
