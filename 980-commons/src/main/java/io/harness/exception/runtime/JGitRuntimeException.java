package io.harness.exception.runtime;

import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class JGitRuntimeException extends RuntimeException {
  private String message;
  Throwable cause;
  ErrorCode code = ErrorCode.INVALID_CREDENTIAL;

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

  public JGitRuntimeException(String message, Throwable cause, ErrorCode code) {
    this.message = message;
    this.cause = cause;
    this.code = code;
  }
}
