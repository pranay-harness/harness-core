package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class YamlException extends WingsException {
  public YamlException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, ErrorCode.GENERAL_YAML_ERROR, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }

  public YamlException(String message, Throwable throwable, EnumSet<ReportTarget> reportTarget) {
    super(message, throwable, ErrorCode.GENERAL_YAML_ERROR, Level.ERROR, reportTarget, null);
    super.getParams().put("message", message);
  }

  public YamlException(String message, ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    super(message, null, errorCode, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}
