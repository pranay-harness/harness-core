package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class FileCreationException extends WingsException {
  public static final String MESSAGE_KEY = "message";

  public FileCreationException(String message, Throwable cause, ErrorCode code, Level level,
      EnumSet<ReportTarget> reportTargets, EnumSet<FailureType> failureTypes) {
    super(message, cause, code, level, reportTargets, failureTypes);
  }
}