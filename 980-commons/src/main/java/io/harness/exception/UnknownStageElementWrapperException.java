package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNKNOWN_STAGE_ELEMENT_WRAPPER_TYPE;

import io.harness.eraro.Level;

public class UnknownStageElementWrapperException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnknownStageElementWrapperException() {
    super(null, null, UNKNOWN_STAGE_ELEMENT_WRAPPER_TYPE, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, "Unknown StageElementWrapper provided during conversion to PipelineExecution");
  }
}
