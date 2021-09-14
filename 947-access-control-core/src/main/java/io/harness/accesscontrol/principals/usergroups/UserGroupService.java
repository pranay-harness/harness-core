/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.principals.usergroups;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface UserGroupService {
  UserGroup upsert(@NotNull @Valid UserGroup resourceGroup);

  PageResponse<UserGroup> list(@NotNull PageRequest pageRequest, @NotEmpty String scopeIdentifier);

  Optional<UserGroup> get(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  UserGroup delete(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  void deleteIfPresent(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);
}
