package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eraro.ErrorCode.AWS_SAM_EXECUTION_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(CDP)
public class AwsSamCommandExecutionException extends WingsException {
  public AwsSamCommandExecutionException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, AWS_SAM_EXECUTION_ERROR, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}