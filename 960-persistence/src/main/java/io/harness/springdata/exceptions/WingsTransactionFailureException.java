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
