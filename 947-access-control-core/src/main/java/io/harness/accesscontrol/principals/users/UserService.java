/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.principals.users;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface UserService {
  long saveAll(@NotNull @Valid List<User> users);

  User createIfNotPresent(@NotNull @Valid User user);

  PageResponse<User> list(@NotNull PageRequest pageRequest, @NotEmpty String scopeIdentifier);

  Optional<User> get(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  Optional<User> deleteIfPresent(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);
}
