package io.harness.exception;

import static io.harness.eraro.ErrorCode.INSTANCE_STATS_PROCESS_ERROR;

import io.harness.eraro.Level;

public class InstanceProcessorException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public InstanceProcessorException(String message) {
    super(message, null, INSTANCE_STATS_PROCESS_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public InstanceProcessorException(String message, Throwable cause) {
    super(message, cause, INSTANCE_STATS_PROCESS_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
