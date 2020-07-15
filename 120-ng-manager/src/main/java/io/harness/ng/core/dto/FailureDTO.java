package io.harness.ng.core.dto;

import io.harness.eraro.ErrorCode;
import io.harness.ng.core.CorrelationContext;
import io.harness.ng.core.Status;
import io.harness.ng.core.ValidationError;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FailureDTO {
  Status status = Status.FAILURE; // we won't rely on http codes, clients will figure out error/success with this field
  ErrorCode code; // enum representing what kind of an error this is (e.g.- secret management error)
  String message; // Short message, something which UI can display directly
  String correlationId;
  List<ValidationError> validationErrors; // used for denoting which fields of the request have errors

  private FailureDTO() {}

  public static FailureDTO toBody(
      Status status, ErrorCode code, String message, List<ValidationError> validationErrors) {
    FailureDTO failureDto = new FailureDTO();
    failureDto.setStatus(status);
    failureDto.setCode(code);
    failureDto.setMessage(message);
    failureDto.setCorrelationId(CorrelationContext.getCorrelationId());
    failureDto.setValidationErrors(validationErrors);
    return failureDto;
  }
}
