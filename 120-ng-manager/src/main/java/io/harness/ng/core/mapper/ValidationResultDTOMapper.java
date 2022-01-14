package io.harness.ng.core.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ValidationResultDTO;

import software.wings.beans.ValidationResult;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ValidationResultDTOMapper {
  public ValidationResult getValidationResultFromDTO(ValidationResultDTO dto) {
    return ValidationResult.builder().valid(dto.isValid()).errorMessage(dto.getErrorMessage()).build();
  }

  public ValidationResultDTO getDTOFromValidationResult(ValidationResult validationResult) {
    return ValidationResultDTO.builder()
        .valid(validationResult.isValid())
        .errorMessage(validationResult.getErrorMessage())
        .build();
  }
}
