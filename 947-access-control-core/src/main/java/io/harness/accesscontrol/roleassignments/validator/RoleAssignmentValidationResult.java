/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roleassignments.validator;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class RoleAssignmentValidationResult {
  ValidationResult scopeValidationResult;
  ValidationResult principalValidationResult;
  ValidationResult roleValidationResult;
  ValidationResult resourceGroupValidationResult;
}
