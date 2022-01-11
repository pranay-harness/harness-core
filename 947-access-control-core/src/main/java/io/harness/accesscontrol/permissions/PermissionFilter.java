/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.permissions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@ApiModel(value = "PermissionListQuery")
public class PermissionFilter {
  @NotNull @Builder.Default Set<String> allowedScopeLevelsFilter = new HashSet<>();
  @NotNull @Builder.Default Set<PermissionStatus> statusFilter = new HashSet<>();
  @NotNull @Builder.Default Set<String> identifierFilter = new HashSet<>();
  @NotNull @Builder.Default IncludedInAllRolesFilter includedInAllRolesFilter = IncludedInAllRolesFilter.NO_FILTER;

  public boolean isEmpty() {
    return allowedScopeLevelsFilter.isEmpty() && statusFilter.isEmpty() && identifierFilter.isEmpty()
        && IncludedInAllRolesFilter.NO_FILTER.equals(includedInAllRolesFilter);
  }

  public enum IncludedInAllRolesFilter {
    PERMISSIONS_INCLUDED_IN_ALL_ROLES,
    PERMISSIONS_NOT_INCLUDED_IN_ALL_ROLES,
    NO_FILTER;
  }
}
