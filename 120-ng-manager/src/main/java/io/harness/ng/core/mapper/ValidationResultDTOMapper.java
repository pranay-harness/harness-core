/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
