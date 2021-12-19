package io.harness.exception;

import static io.harness.eraro.ErrorCode.APPROVAL_STEP_NG_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import lombok.Getter;

@OwnedBy(HarnessTeam.CDC)
public class ApprovalStepNGException extends WingsException {
  @Getter private final boolean fatal;

  public ApprovalStepNGException(String message, boolean fatal, Throwable cause) {
    super(message, cause, APPROVAL_STEP_NG_ERROR, Level.ERROR, null, null);
    super.param("message", message);
    this.fatal = fatal;
  }

  public ApprovalStepNGException(String message, boolean fatal) {
    this(message, fatal, null);
  }

  public ApprovalStepNGException(String message) {
    this(message, false, null);
  }
}
