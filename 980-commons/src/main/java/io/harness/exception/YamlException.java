/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class YamlException extends WingsException {
  public YamlException(String message) {
    super(message, null, ErrorCode.GENERAL_YAML_ERROR, Level.ERROR, null, null);
    super.getParams().put("message", message);
  }

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
