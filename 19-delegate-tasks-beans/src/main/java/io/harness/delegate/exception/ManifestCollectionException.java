package io.harness.exception;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class ManifestCollectionException extends WingsException {
  public ManifestCollectionException(String message) {
    super(message, null, GENERAL_ERROR, Level.ERROR, null, null);
  }

  public ManifestCollectionException(String message, Throwable cause) {
    super(message, cause, GENERAL_ERROR, Level.ERROR, null, null);
  }

  public ManifestCollectionException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, GENERAL_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }

  public ManifestCollectionException(String message, EnumSet<ReportTarget> reportTargets, Throwable t) {
    super(message, t, GENERAL_ERROR, Level.ERROR, reportTargets, null);
    super.param("message", message);
  }
}
