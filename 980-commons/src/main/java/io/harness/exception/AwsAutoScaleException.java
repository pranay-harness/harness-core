package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;
import java.util.Set;

public class AwsAutoScaleException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public AwsAutoScaleException(String message, ErrorCode errorCode, Set<ReportTarget> reportTargets) {
    super(message, null, errorCode, Level.ERROR, (EnumSet) reportTargets, null);
    super.param(MESSAGE_ARG, message);
  }
}
