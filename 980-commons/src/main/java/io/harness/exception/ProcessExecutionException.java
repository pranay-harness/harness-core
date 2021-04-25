package io.harness.exception;

import static io.harness.eraro.ErrorCode.PROCESS_EXECUTION_EXCEPTION;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class ProcessExecutionException extends WingsException {
  public ProcessExecutionException(String message, Throwable cause) {
    super(message, cause, PROCESS_EXECUTION_EXCEPTION, Level.ERROR, USER, EnumSet.of(FailureType.APPLICATION_ERROR));
  }
}
