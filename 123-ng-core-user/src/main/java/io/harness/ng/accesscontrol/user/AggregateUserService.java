/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.user.remote.dto.UserAggregateDTO;

@OwnedBy(PL)
public interface AggregateUserService {
  PageResponse<UserAggregateDTO> getAggregatedUsers(Scope scope, String searchTerm, PageRequest pageRequest);

  PageResponse<UserAggregateDTO> getAggregatedUsers(
      Scope scope, ACLAggregateFilter aclAggregateFilter, PageRequest pageRequest);

  UserAggregateDTO getAggregatedUser(Scope scope, String userId);
}
