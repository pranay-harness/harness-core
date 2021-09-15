/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.permissions.persistence;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface PermissionDao {
  Permission create(@NotNull @Valid Permission permission);

  List<Permission> list(@NotNull @Valid PermissionFilter permissionFilter);

  Optional<Permission> get(@NotEmpty String identifier);

  Permission update(@Valid Permission permission);

  Permission delete(@NotEmpty String identifier);
}
