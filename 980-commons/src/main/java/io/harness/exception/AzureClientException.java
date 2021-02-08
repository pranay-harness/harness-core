package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class AzureClientException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public AzureClientException(String message, ErrorCode code, EnumSet<WingsException.ReportTarget> reportTargets) {
    super(message, null, code, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }
}
