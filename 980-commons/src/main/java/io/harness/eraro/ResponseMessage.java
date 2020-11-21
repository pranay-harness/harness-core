package io.harness.eraro;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.Level.ERROR;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class ResponseMessage {
  @Default private ErrorCode code = DEFAULT_ERROR_CODE;
  @Default private Level level = ERROR;

  private String message;
  private Throwable exception;
}
