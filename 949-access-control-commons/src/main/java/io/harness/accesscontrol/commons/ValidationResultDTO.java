package io.harness.accesscontrol.commons;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "ValidationResult")
public class ValidationResultDTO {
  boolean isValid;
  String errorMessage;
}
