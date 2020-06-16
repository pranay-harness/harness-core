package io.harness.eraro;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.Level.ERROR;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class ResponseMessage {
  @Default private ErrorCode code = DEFAULT_ERROR_CODE;
  @Default private Level level = ERROR;

  private String message;
  private Throwable exception;
}
