package io.harness.delegate.task.pcf.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.CDP)
public class InvalidPcfStateException extends WingsException {
  public InvalidPcfStateException(String message, ErrorCode code, EnumSet<ReportTarget> reportTarget) {
    super(message, null, code, Level.ERROR, reportTarget, null);
    param("message", message);
  }
}
