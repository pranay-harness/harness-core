package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class CacheNotFoundException extends WingsException {
  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public CacheNotFoundException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, ErrorCode.CACHE_NOT_FOUND_EXCEPTION, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}
