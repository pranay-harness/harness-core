package io.harness.exception;

import static io.harness.eraro.ErrorCode.HTTP_RESPONSE_EXCEPTION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import lombok.Getter;

@OwnedBy(HarnessTeam.PIPELINE)
public class HttpResponseException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  @Getter private final int statusCode;
  @Getter private final String responseMessage;

  public HttpResponseException(int statusCode, String responseMessage) {
    super(String.format("Unsuccessful HTTP call: status code = %d, message = %s", statusCode, responseMessage), null,
        HTTP_RESPONSE_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, getMessage());
    this.statusCode = statusCode;
    this.responseMessage = responseMessage;
  }
}
