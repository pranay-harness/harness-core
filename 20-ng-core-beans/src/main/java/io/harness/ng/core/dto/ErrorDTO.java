package io.harness.ng.core.dto;

import io.harness.eraro.ErrorCode;
import io.harness.ng.core.CorrelationContext;
import io.harness.ng.core.Status;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel(value = "Error")
public class ErrorDTO {
  Status status = Status.ERROR; // we won't rely on http codes, clients will figure out error/success with this field
  ErrorCode code; // enum representing what kind of an error this is (e.g.- secret management error)
  String message; // Short message, something which UI can display directly
  String correlationId; // for distributed tracing
  String detailedMessage; // used to send detailed message in case of an error from Harness end for debugging

  private ErrorDTO() {}

  public static ErrorDTO newError(Status status, ErrorCode code, String message, String detailedMessage) {
    ErrorDTO errorDto = new ErrorDTO();
    errorDto.setStatus(status);
    errorDto.setCode(code);
    errorDto.setMessage(message);
    errorDto.setDetailedMessage(detailedMessage);
    errorDto.setCorrelationId(CorrelationContext.getCorrelationId());
    return errorDto;
  }

  public static ErrorDTO newError(Status status, ErrorCode code, String message) {
    return newError(status, code, message, null);
  }
}
