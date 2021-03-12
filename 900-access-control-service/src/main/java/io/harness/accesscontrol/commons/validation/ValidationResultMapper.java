package io.harness.accesscontrol.commons.validation;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.commons.ValidationResultDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationResultMapper {
  public static ValidationResultDTO toDTO(ValidationResult result) {
    if (result == null) {
      return null;
    }
    return ValidationResultDTO.builder().isValid(result.isValid()).errorMessage(result.getErrorMessage()).build();
  }
}
