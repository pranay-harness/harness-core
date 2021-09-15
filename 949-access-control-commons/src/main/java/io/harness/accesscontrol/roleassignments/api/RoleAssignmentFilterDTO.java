/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roleassignments.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "RoleAssignmentFilter")
@OwnedBy(PL)
public class RoleAssignmentFilterDTO {
  Set<String> resourceGroupFilter;
  Set<String> roleFilter;
  Set<PrincipalType> principalTypeFilter;
  Set<PrincipalDTO> principalFilter;
  Set<Boolean> harnessManagedFilter;
  Set<Boolean> disabledFilter;
}
