/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roleassignments.validator;

import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class RoleAssignmentValidationRequest {
  @NotNull @Valid RoleAssignment roleAssignment;
  @Builder.Default boolean validateScope = true;
  @Builder.Default boolean validatePrincipal = true;
  @Builder.Default boolean validateRole = true;
  @Builder.Default boolean validateResourceGroup = true;
}
