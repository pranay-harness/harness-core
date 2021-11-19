package io.harness.exception.runtime;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@OwnedBy(DX)
@EqualsAndHashCode(callSuper = true)
public class JGitRuntimeException extends RuntimeException {
  private String message;
  Throwable cause;
  ErrorCode code = ErrorCode.INVALID_CREDENTIAL;
  String commitId;
  String branch;

  public JGitRuntimeException(String message) {
    this.message = message;
  }

  public JGitRuntimeException(String message, ErrorCode code) {
    this.message = message;
    this.code = code;
  }

  public JGitRuntimeException(String message, Throwable cause) {
    this.message = message;
    this.cause = cause;
  }

  public JGitRuntimeException(String message, Throwable cause, ErrorCode code, String commitId, String branch) {
    this.message = message;
    this.cause = cause;
    this.code = code;
    this.commitId = commitId;
    this.branch = branch;
  }
}
