/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.resources.resourcetypes.ResourceType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface PermissionService {
  Permission create(@NotNull @Valid Permission permission);

  Optional<Permission> get(@NotEmpty String identifier);

  List<Permission> list(@NotNull @Valid PermissionFilter permissionFilter);

  Permission update(@Valid Permission permission);

  Permission delete(@NotEmpty String identifier);

  Optional<ResourceType> getResourceTypeFromPermission(@NotNull @Valid Permission permission);
}
