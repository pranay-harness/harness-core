/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dto;

import io.harness.eraro.ErrorCode;
import io.harness.ng.core.CorrelationContext;
import io.harness.ng.core.Status;
import io.harness.ng.core.ValidationError;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel(value = "Failure")
public class FailureDTO {
  Status status = Status.FAILURE; // we won't rely on http codes, clients will figure out error/success with this field
  ErrorCode code; // enum representing what kind of an error this is (e.g.- secret management error)
  String message; // Short message, something which UI can display directly
  String correlationId;
  List<ValidationError> errors; // used for denoting which fields of the request have errors

  private FailureDTO() {}

  public static FailureDTO toBody(
      Status status, ErrorCode code, String message, List<ValidationError> validationErrors) {
    FailureDTO failureDto = new FailureDTO();
    failureDto.setStatus(status);
    failureDto.setCode(code);
    failureDto.setMessage(message);
    failureDto.setCorrelationId(CorrelationContext.getCorrelationId());
    failureDto.setErrors(validationErrors);
    return failureDto;
  }
}
