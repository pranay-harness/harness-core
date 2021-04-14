package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(CDP)
public class HelmClientException extends WingsException {
  public HelmClientException(String message) {
    super(message, null, GENERAL_ERROR, Level.ERROR, null, null);
    super.param("message", message);
  }

  public HelmClientException(String message, Throwable cause) {
    super(message, cause, GENERAL_ERROR, Level.ERROR, null, null);
    super.param("message", message);
  }

  public HelmClientException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, GENERAL_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }

  public HelmClientException(String message, EnumSet<ReportTarget> reportTargets, Throwable t) {
    super(message, t, GENERAL_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }
}
