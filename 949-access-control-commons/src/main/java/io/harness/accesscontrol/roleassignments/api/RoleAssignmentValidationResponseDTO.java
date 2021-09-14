/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.commons.ValidationResultDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@ApiModel(value = "RoleAssignmentValidationResponse")
public class RoleAssignmentValidationResponseDTO {
  ValidationResultDTO principalValidationResult;
  ValidationResultDTO roleValidationResult;
  ValidationResultDTO resourceGroupValidationResult;
}
