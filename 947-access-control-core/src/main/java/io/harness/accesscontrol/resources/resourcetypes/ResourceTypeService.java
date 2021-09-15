/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.resources.resourcetypes;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface ResourceTypeService {
  ResourceType save(@NotNull ResourceType resourceType);
  Optional<ResourceType> get(@NotEmpty String identifier);
  Optional<ResourceType> getByPermissionKey(@NotEmpty String permissionKey);
  List<ResourceType> list();
}
