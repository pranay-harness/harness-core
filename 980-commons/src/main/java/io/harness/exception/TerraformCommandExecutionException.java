/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eraro.ErrorCode.TERRAFORM_EXECUTION_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(CDP)
public class TerraformCommandExecutionException extends WingsException {
  public TerraformCommandExecutionException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, TERRAFORM_EXECUTION_ERROR, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }

  public TerraformCommandExecutionException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, TERRAFORM_EXECUTION_ERROR, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}
