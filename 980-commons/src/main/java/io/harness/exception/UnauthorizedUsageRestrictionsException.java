package io.harness.exception;

import static io.harness.eraro.ErrorCode.USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class UnauthorizedUsageRestrictionsException extends WingsException {
  public UnauthorizedUsageRestrictionsException(EnumSet<ReportTarget> reportTarget) {
    super(null, null, USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, Level.ERROR, reportTarget, null);
  }
}
