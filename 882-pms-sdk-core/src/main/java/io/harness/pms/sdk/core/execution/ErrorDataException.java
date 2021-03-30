package io.harness.pms.sdk.core.execution;

import static io.harness.eraro.ErrorCode.TASK_FAILURE_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.tasks.ErrorResponseData;

import lombok.Getter;

public class ErrorDataException extends WingsException {
  @Getter ErrorResponseData errorResponseData;

  public ErrorDataException(ErrorResponseData errorResponseData) {
    super(errorResponseData.getErrorMessage(), null, TASK_FAILURE_ERROR, Level.ERROR, null,
        errorResponseData.getFailureTypes());
    this.errorResponseData = errorResponseData;
  }
}
