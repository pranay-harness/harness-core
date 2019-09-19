package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import lombok.Builder;

import java.util.EnumSet;

public class NoResultFoundException extends WingsException {
  @Builder(builderMethodName = "newBuilder")
  protected NoResultFoundException(String message, Throwable cause, ErrorCode code, Level level,
      EnumSet<ReportTarget> reportTargets, EnumSet<FailureType> failureTypes) {
    super(message, cause, code, level, reportTargets, failureTypes);
  }
}
