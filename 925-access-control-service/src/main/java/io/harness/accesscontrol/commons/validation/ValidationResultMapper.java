/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.commons.validation;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.commons.ValidationResultDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class ValidationResultMapper {
  public static ValidationResultDTO toDTO(ValidationResult result) {
    if (result == null) {
      return null;
    }
    return ValidationResultDTO.builder().isValid(result.isValid()).errorMessage(result.getErrorMessage()).build();
  }
}
