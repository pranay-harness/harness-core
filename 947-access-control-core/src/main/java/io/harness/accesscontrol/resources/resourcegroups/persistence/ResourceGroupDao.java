/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface ResourceGroupDao {
  ResourceGroup upsert(@NotNull @Valid ResourceGroup resourceGroup);

  PageResponse<ResourceGroup> list(@NotNull PageRequest pageRequest, @NotEmpty String scopeIdentifier);

  List<ResourceGroup> list(List<String> resourceGroupIdentifier, String scopeIdentifier);

  Optional<ResourceGroup> get(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  Optional<ResourceGroup> delete(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);
}
