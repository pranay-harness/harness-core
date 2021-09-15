/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.exceptions;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class NGYamlParsingException extends WingsException {
  public NGYamlParsingException(String message, EnumSet<ReportTarget> reportTarget) {
    super(message, null, ErrorCode.INVALID_YAML_PAYLOAD, Level.ERROR, reportTarget, null);
    super.getParams().put("message", message);
  }

  public NGYamlParsingException(String message) {
    super(message, null, ErrorCode.INVALID_YAML_PAYLOAD, Level.ERROR, null, null);
    super.getParams().put("message", message);
  }
}
