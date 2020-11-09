package io.harness.exception;

import static io.harness.eraro.ErrorCode.FEATURE_UNAVAILABLE;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class UnavailableFeatureException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnavailableFeatureException(String message, EnumSet<ReportTarget> reportTargets) {
    super(null, null, FEATURE_UNAVAILABLE, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }
}
